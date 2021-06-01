/*
 * Copyright (c) 2021, Red Hat Inc. All rights reserved.
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

import com.sun.jna.*;
import org.openjdk.jcstress.vm.VMSupport;

import java.util.Collections;
import java.util.List;

public class AffinitySupport {

    public static void bind(int cpu) {
        if (VMSupport.isLinux()) {
            Linux.bind(cpu);
        } else {
            throw new IllegalStateException("Not implemented");
        }
    }

    public static void tryBind() {
        if (VMSupport.isLinux()) {
            Linux.tryBind();
        } else {
            throw new IllegalStateException("Not implemented");
        }
    }

    static class Linux {
        private static volatile CLibrary INSTANCE;
        private static boolean BIND_TRIED;

        public static void tryInit() {
            if (INSTANCE == null) {
                synchronized (Linux.class) {
                    if (INSTANCE == null) {
                        INSTANCE = Native.load("c", CLibrary.class);
                    }
                }
            }
        }

        public static void bind(int cpu) {
            tryInit();

            final cpu_set_t cpuset = new cpu_set_t();
            cpuset.set(cpu);

            set(cpuset);
        }

        public static void tryBind() {
            if (BIND_TRIED) return;

            synchronized (Linux.class) {
                if (BIND_TRIED) return;

                tryInit();

                final cpu_set_t new_cpuset = new cpu_set_t();
                new_cpuset.set(0);
                final cpu_set_t old_cpuset = new cpu_set_t();

                get(old_cpuset);
                set(new_cpuset);
                set(old_cpuset);

                BIND_TRIED = true;
            }
        }

        private static void get(cpu_set_t cpuset) {
            try {
                if (INSTANCE.sched_getaffinity(0, cpu_set_t.SIZE_OF, cpuset) != 0) {
                    throw new IllegalStateException("Failed: " + Native.getLastError());
                }
            } catch (LastErrorException e) {
                throw new IllegalStateException("Failed: " + Native.getLastError());
            }
        }

        private static void set(cpu_set_t cpuset) {
            try {
                if (INSTANCE.sched_setaffinity(0, cpu_set_t.SIZE_OF, cpuset) != 0) {
                    throw new IllegalStateException("Failed: " + Native.getLastError());
                }
            } catch (LastErrorException e) {
                throw new IllegalStateException("Failed: " + Native.getLastError());
            }
        }

        interface CLibrary extends Library {
            int sched_getaffinity(int pid, int size, cpu_set_t cpuset) throws LastErrorException;
            int sched_setaffinity(int pid, int size, cpu_set_t cpuset) throws LastErrorException;
        }

        public static class cpu_set_t extends Structure {
            private static final int CPUSET_SIZE = 1024;
            private static final int NCPU_BITS = 8 * NativeLong.SIZE;
            private static final int SIZE_OF = (CPUSET_SIZE / NCPU_BITS) * NativeLong.SIZE;

            public NativeLong[] __bits = new NativeLong[CPUSET_SIZE / NCPU_BITS];

            public cpu_set_t() {
                for (int i = 0; i < __bits.length; i++) {
                    __bits[i] = new NativeLong(0);
                }
            }

            public void set(int cpu) {
                int cIdx = cpu / NCPU_BITS;
                long mask = 1L << (cpu % NCPU_BITS);
                NativeLong bit = __bits[cIdx];
                bit.setValue(bit.longValue() | mask);
            }

            @Override
            protected List<String> getFieldOrder() {
                return Collections.singletonList("__bits");
            }
        }
    }

}
