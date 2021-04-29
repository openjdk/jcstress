/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jcstress.os;

import org.openjdk.jcstress.util.InputStreamDrainer;
import org.openjdk.jcstress.vm.VMSupportException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OSSupport {

    private static volatile boolean TASKSET_AVAILABLE;
    public static boolean taskSetAvailable() {
        return TASKSET_AVAILABLE;
    }

    private static volatile boolean AFFINITY_SUPPORT_AVAILABLE;
    public static boolean affinitySupportAvailable() {
        return AFFINITY_SUPPORT_AVAILABLE;
    }

    public static void init() {
        System.out.println("Probing the target OS:");
        System.out.println(" (all failures are non-fatal, but may affect testing accuracy)");
        System.out.println();

        TASKSET_AVAILABLE = detectCommand("Trying to set global affinity with taskset",
                "taskset", "-c", "0");

        try {
            AffinitySupport.tryBind();
            System.out.printf("----- %s %s%n", "[OK]", "Trying to set per-thread affinity with syscalls");
            AFFINITY_SUPPORT_AVAILABLE = true;
        } catch (Throwable e) {
            System.out.printf("----- %s %s%n", "[N/A]", "Trying to set per-thread affinity with syscalls");
            System.out.println(e.getMessage());
            AFFINITY_SUPPORT_AVAILABLE = false;
        }

        System.out.println();
    }

    private static boolean detectCommand(String label, String... opts) {
        try {
            tryWith(opts);
            System.out.printf("----- %s %s%n", "[OK]", label);
            return true;
        } catch (VMSupportException ex) {
            System.out.printf("----- %s %s%n", "[N/A]", label);
            System.out.println(ex.getMessage());
            return false;
        }
    }

    public static void tryWith(String[] commands) throws VMSupportException {
        try {
            List<String> commandString = new ArrayList<>();

            commandString.addAll(
                    Arrays.stream(commands)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList()));

            commandString.add("echo");

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

}
