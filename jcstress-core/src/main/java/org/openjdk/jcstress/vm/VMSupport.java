/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jcstress.vm;

import org.openjdk.jcstress.Main;
import org.openjdk.jcstress.util.InputStreamDrainer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class VMSupport {

    private static final List<String> ADD_JVM_FLAGS = new ArrayList<>();
    private static final Collection<Collection<String>> AVAIL_JVM_MODES = new ArrayList<>();

    public static void initSupport() {
        System.out.println("Initializing and probing the target VM: ");
        System.out.println(" (all failures are non-fatal, but may affect testing accuracy)");
        System.out.println();

        String jarName = new File(Main.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getPath();

        detect("Adding ourselves to bootclasspath: " + jarName,
                "-Xbootclasspath/a:" + jarName,
                PrivilegedTestMain.class);

        detect("Unlocking diagnostic VM options",
                "-XX:+UnlockDiagnosticVMOptions",
                SimpleTestMain.class);

        detect("Testing @Contended works on all results",
                "-XX:-RestrictContended",
                ContendedTestMain.class);

        detect("Unlocking Whitebox API for online de-optimization",
                "-XX:+WhiteBoxAPI",
                DeoptTestMain.class);

        System.out.println();
    }

    private static void detect(String label, String opt, Class<?> mainClass) {
        try {
            tryWith(opt, mainClass.getName());
            ADD_JVM_FLAGS.add(opt);
            System.out.printf("----- %s %s%n", "[OK]", label);
        } catch (VMSupportException ex) {
            System.out.printf("----- %s %s%n", "[FAILED]", label);
            System.out.println(ex.getMessage());
        }
    }

    public static void detectAvailableVMModes(Collection<String> jvmArgs) {
        Collection<Collection<String>> modes;

        if (jvmArgs != null) {
            modes = Collections.singleton(jvmArgs);
        } else {
            modes = Arrays.asList(
                    // Intepreted
                    Arrays.asList("-Xint"),

                    // Tiered C1
                    Arrays.asList("-XX:TieredStopAtLevel=1"),

                    // Non-tiered C1
                    Arrays.asList("-client"),
                    Arrays.asList("-client", "-XX:-TieredCompilation"),

                    // Tiered C2
                    Arrays.asList("-server"),
                    Arrays.asList("-server", "-XX:+UnlockDiagnosticVMOptions", "-XX:+StressLCM", "-XX:+StressGCM"),

                    // Non-tiered C2
                    Arrays.asList("-server", "-XX:-TieredCompilation"),
                    Arrays.asList("-server", "-XX:-TieredCompilation", "-XX:+UnlockDiagnosticVMOptions", "-XX:+StressLCM", "-XX:+StressGCM"));
        }

        System.out.println("Probing what VM modes are available:");
        System.out.println(" (failures are non-fatal, but may miss some interesting cases)");
        System.out.println();
        for (Collection<String> mode : modes) {
            try {
                List<String> line = new ArrayList<>(mode);
                line.add(SimpleTestMain.class.getName());
                tryWith(line.toArray(new String[0]));
                AVAIL_JVM_MODES.add(mode);
                System.out.printf("   [OK] %s%n", mode);
            } catch (VMSupportException e) {
                System.out.printf("  [N/A] %s%n", mode);
            }
        }
        System.out.println();
    }

    public static void tryWith(String... lines) throws VMSupportException {
        try {
            List<String> commandString = getJavaInvokeLine();
            commandString.addAll(Arrays.asList(lines));

            ProcessBuilder pb = new ProcessBuilder(commandString);
            Process p = pb.start();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream(), baos);
            InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream(), baos);

            errDrainer.start();
            outDrainer.start();

            int ecode = p.waitFor();

            errDrainer.join();
            outDrainer.join();

            if (ecode != 0) {
                String msg = new String(baos.toByteArray());
                throw new VMSupportException(msg);
            }
        } catch (IOException | InterruptedException ex) {
            throw new VMSupportException(ex.getMessage());
        }
    }

    public static List<String> getJavaInvokeLine() {
        List<String> command = new ArrayList<>();

        // jvm path
        command.add(getDefaultJvm());

        // jvm classpath
        command.add("-cp");
        if (isWindows()) {
            command.add('"' + System.getProperty("java.class.path") + '"');
        } else {
            command.add(System.getProperty("java.class.path"));
        }

        command.addAll(ADD_JVM_FLAGS);

        return command;
    }

    private static String getDefaultJvm() {
        StringBuilder javaExecutable = new StringBuilder();
        javaExecutable.append(System.getProperty("java.home"));
        javaExecutable.append(File.separator);
        javaExecutable.append("bin");
        javaExecutable.append(File.separator);
        javaExecutable.append("java");
        javaExecutable.append(isWindows() ? ".exe" : "");
        return javaExecutable.toString();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").contains("indows");
    }


    public static Collection<Collection<String>> getAvailableVMModes() {
        return AVAIL_JVM_MODES;
    }

    /**
     * Warm up the CPU schedulers, bring all the CPUs online to get the
     * reasonable estimate of the system capacity.
     *
     * @return online CPU count
     */
    public static int figureOutHotCPUs() {
        ExecutorService service = Executors.newCachedThreadPool();

        System.out.print("Burning up to figure out the exact CPU count...");

        int warmupTime = 1000;
        long lastChange = System.currentTimeMillis();

        List<Future<?>> futures = new ArrayList<>();
        futures.add(service.submit(new BurningTask()));

        System.out.print(".");

        int max = 0;
        while (System.currentTimeMillis() - lastChange < warmupTime) {
            int cur = Runtime.getRuntime().availableProcessors();
            if (cur > max) {
                System.out.print(".");
                max = cur;
                lastChange = System.currentTimeMillis();
                futures.add(service.submit(new BurningTask()));
            }
        }

        for (Future<?> f : futures) {
            System.out.print(".");
            f.cancel(true);
        }

        service.shutdown();

        System.out.println(" done!");
        System.out.println();

        return max;
    }

    static class BurningTask implements Runnable {

        @Override
        public void run() {
            while (!Thread.interrupted()); // burn;
        }
    }

}
