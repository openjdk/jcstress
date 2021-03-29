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

import org.openjdk.jcstress.Options;
import org.openjdk.jcstress.util.ArrayUtils;
import org.openjdk.jcstress.util.FileUtils;
import org.openjdk.jcstress.util.InputStreamDrainer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class VMSupport {

    private static final List<String> GLOBAL_JVM_FLAGS = new ArrayList<>();
    private static final List<String> C2_STRESS_JVM_FLAGS = new ArrayList<>();
    private static final List<String> C2_ONLY_STRESS_JVM_FLAGS = new ArrayList<>();

    private static final List<Config> AVAIL_JVM_CONFIGS = new ArrayList<>();
    private static volatile boolean THREAD_SPIN_WAIT_AVAILABLE;
    private static volatile boolean COMPILER_DIRECTIVES_AVAILABLE;
    private static volatile boolean PRINT_ASSEMBLY_AVAILABLE;

    public static boolean spinWaitHintAvailable() {
        return THREAD_SPIN_WAIT_AVAILABLE;
    }

    public static boolean compilerDirectivesAvailable() {
        return COMPILER_DIRECTIVES_AVAILABLE;
    }

    public static boolean printAssemblyAvailable() {
        return PRINT_ASSEMBLY_AVAILABLE;
    }

    public static void initFlags(Options opts) {
        System.out.println("Initializing and probing the target VM: ");
        System.out.println(" (all failures are non-fatal, but may affect testing accuracy)");
        System.out.println();

        detect("Unlocking diagnostic VM options",
                SimpleTestMain.class,
                GLOBAL_JVM_FLAGS,
                "-XX:+UnlockDiagnosticVMOptions"
        );

        // Tests are supposed to run in a very tight memory constraints:
        // the test objects are small and reused where possible. The footprint
        // testing machinery would select appropriate stride sizes to fit the heap.
        // Users can override this to work on smaller/larger machines, but it should
        // not be necessary, as even the smallest machines usually have more than 256M
        // of system memory per CPU.

        int heap = opts.getHeapPerForkMb();
        detect("Trimming down the VM heap size to " + heap + "M",
                SimpleTestMain.class,
                GLOBAL_JVM_FLAGS,
                "-Xms" + heap + "M", "-Xmx" + heap + "M");

        // The tests are usually not GC heavy. The minimum amount of threads a jcstress
        // test uses is 2, so we can expect the CPU affinity machinery to allocate at
        // least 2 CPUs per fork. This gives us the upper bound for the number of GC threads: 2,
        // otherwise we risk oversubscribing the forked VM.
        //
        // We could, theoretically, drop the number of GC threads to 1, but GC ergonomics
        // sometimes decides to switch to single-threaded mode in some GC implementations
        // (e.g. for reference processing), and it would make sense to let GC run in multi-threaded
        // modes instead.

        detect("Trimming down the number of parallel GC threads",
                SimpleTestMain.class,
                GLOBAL_JVM_FLAGS,
                "-XX:ParallelGCThreads=2"
        );

        detect("Trimming down the number of concurrent GC threads",
                SimpleTestMain.class,
                GLOBAL_JVM_FLAGS,
                "-XX:ConcGCThreads=2"
        );

        detect("Trimming down the number of G1 concurrent refinement GC threads",
                SimpleTestMain.class,
                GLOBAL_JVM_FLAGS,
                "-XX:G1ConcRefinementThreads=2"
        );

        detect("Trimming down the number of compiler threads",
                SimpleTestMain.class,
                GLOBAL_JVM_FLAGS,
                "-XX:CICompilerCount=2" // This is the absolute minimum for tiered configurations
        );

        detect("Testing @Contended works on all results and infra objects",
                ContendedTestMain.class,
                GLOBAL_JVM_FLAGS,
                "-XX:-RestrictContended"
        );

        try {
            String whiteBoxJarName = FileUtils.copyFileToTemp("/whitebox-api.jar", "whitebox", ".jar");
            detect("Unlocking Whitebox API for online de-optimization: all methods",
                    DeoptAllTestMain.class,
                    GLOBAL_JVM_FLAGS,
                    "-XX:+WhiteBoxAPI", "-Xbootclasspath/a:" + whiteBoxJarName
            );
            detect("Unlocking Whitebox API for online de-optimization: selected methods",
                    DeoptMethodTestMain.class,
                    GLOBAL_JVM_FLAGS,
                    "-XX:+WhiteBoxAPI", "-Xbootclasspath/a:" + whiteBoxJarName
            );
        } catch (IOException e) {
            throw new IllegalStateException("Fatal error: WhiteBoxAPI JAR problems.", e);
        }

        detect("Unlocking debug information for non-safepoints",
                SimpleTestMain.class,
                GLOBAL_JVM_FLAGS,
                "-XX:+DebugNonSafepoints"
        );

        detect("Unlocking C2 local code motion randomizer",
                SimpleTestMain.class,
                C2_STRESS_JVM_FLAGS,
                "-XX:+StressLCM"
        );

        detect("Unlocking C2 global code motion randomizer",
                SimpleTestMain.class,
                C2_STRESS_JVM_FLAGS,
                "-XX:+StressGCM"
        );

        detect("Unlocking C2 iterative global value numbering randomizer",
                SimpleTestMain.class,
                C2_STRESS_JVM_FLAGS,
                "-XX:+StressIGVN"
        );

        detect("Unlocking C2 conditional constant propagation randomizer",
                SimpleTestMain.class,
                C2_STRESS_JVM_FLAGS,
                "-XX:+StressCCP"
        );

        C2_ONLY_STRESS_JVM_FLAGS.add("-XX:-TieredCompilation");
        C2_ONLY_STRESS_JVM_FLAGS.addAll(C2_STRESS_JVM_FLAGS);

        detect("Testing allocation profiling",
                AllocProfileMain.class,
                null
        );

        THREAD_SPIN_WAIT_AVAILABLE =
                detect("Testing Thread.onSpinWait",
                        ThreadSpinWaitTestMain.class,
                        null
                );

        PRINT_ASSEMBLY_AVAILABLE =
                detect("Testing PrintAssembly",
                        SimpleTestMain.class,
                        null,
                        "-XX:+PrintAssembly"
                );

        try {
            File temp = File.createTempFile("jcstress", "directives");

            PrintWriter pw = new PrintWriter(temp);
            pw.println("[ { match: \"*::*\", PrintInlining: true } ]");
            pw.close();

            COMPILER_DIRECTIVES_AVAILABLE =
                    detect("Testing compiler directives",
                            SimpleTestMain.class,
                            null,
                            "-XX:CompilerDirectivesFile=" + temp.getAbsolutePath()
                    );

            temp.delete();
        } catch (IOException e) {
            // Do nothing.
        }

        System.out.println();
    }

    private static boolean detect(String label, Class<?> mainClass, List<String> list, String... opts) {
        try {
            String[] arguments = ArrayUtils.concat(opts, mainClass.getName());
            tryWith(arguments);
            if (list != null) {
                list.addAll(Arrays.asList(opts));
            }
            System.out.printf("----- %s %s%n", "[OK]", label);
            return true;
        } catch (VMSupportException ex) {
            System.out.printf("----- %s %s%n", "[FAILED]", label);
            System.out.println(ex.getMessage());
            return false;
        }
    }

    public static void detectAvailableVMConfigs(boolean splitCompilation, List<String> jvmArgs, List<String> jvmArgsPrepend) {
        System.out.println("Probing what VM configurations are available:");
        System.out.println(" (failures are non-fatal, but may miss some interesting cases)");

        List<Config> configs;

        if (!jvmArgs.isEmpty()) {
            configs = Collections.singletonList(new Config(jvmArgs, false));
        } else if (splitCompilation && COMPILER_DIRECTIVES_AVAILABLE) {
            System.out.println(" (split compilation is requested and compiler directives are available)");
            configs = Arrays.asList(
                    // Default global
                    new Config(Collections.emptyList(), false),

                    // C2 compilations stress
                    new Config(C2_STRESS_JVM_FLAGS, true)
            );
        } else {
            configs = Arrays.asList(
                    // Interpreted
                    new Config(Arrays.asList("-Xint"), false),

                    // C1
                    new Config(Arrays.asList("-XX:TieredStopAtLevel=1"), false),

                    // C2
                    new Config(Arrays.asList("-XX:-TieredCompilation"), false),

                    // C2 only + stress
                    new Config(C2_ONLY_STRESS_JVM_FLAGS, true)
            );
        }

        // Mix in input arguments, if available
        List<String> inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        if (!inputArgs.isEmpty()) {
            configs = configs.stream().map(c -> {
                List<String> l = new ArrayList<>();
                l.addAll(inputArgs);
                l.addAll(c.args());
                return new Config(l, c.onlyIfC2());
            }).collect(Collectors.toList());
        }

        // Mix in prepends, if available
        if (jvmArgsPrepend != null) {
            configs = configs.stream().map(c -> {
                List<String> l = new ArrayList<>();
                l.addAll(jvmArgsPrepend);
                l.addAll(c.args());
                return new Config(l, c.onlyIfC2());
            }).collect(Collectors.toList());
        }

        System.out.println();

        for (Config config : configs) {
            List<String> args = config.args();
            try {
                List<String> line = new ArrayList<>(args);
                line.add(SimpleTestMain.class.getName());
                tryWith(line.toArray(new String[0]));
                AVAIL_JVM_CONFIGS.add(config);
                System.out.printf("----- [OK] %s%n", args);
            } catch (VMSupportException e) {
                System.out.printf("----- [N/A] %s%n", args);
                System.out.println(e.getMessage());
                System.out.println();
            }
        }
        System.out.println();
    }

    public static void tryWith(String... lines) throws VMSupportException {
        try {
            List<String> commandString = getJavaInvokeLine();
            commandString.addAll(
                    Arrays.stream(lines)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList()));

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

        command.addAll(GLOBAL_JVM_FLAGS);

        return command;
    }

    private static String getDefaultJvm() {
        return System.getProperty("java.home") +
                File.separator +
                "bin" +
                File.separator +
                "java" +
                (isWindows() ? ".exe" : "");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").contains("indows");
    }


    public static List<Config> getAvailableVMConfigs() {
        return AVAIL_JVM_CONFIGS;
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
            while (!Thread.interrupted()) ; // burn;
        }
    }

    public static class Config {
        private final List<String> args;
        private final boolean onlyIfC2;

        private Config(List<String> args, boolean onlyIfC2) {
            this.args = args;
            this.onlyIfC2 = onlyIfC2;
        }

        public boolean onlyIfC2() {
            return onlyIfC2;
        }

        public List<String> args() {
            return args;
        }
    }

}
