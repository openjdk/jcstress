/*
 * Copyright (c) 2017, Red Hat Inc. All rights reserved.
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

package org.openjdk.jcstress;

import org.openjdk.jcstress.infra.runners.ForkedTestConfig;
import org.openjdk.jcstress.os.AffinityMode;
import org.openjdk.jcstress.os.NodeType;
import org.openjdk.jcstress.os.SchedulingClass;
import org.openjdk.jcstress.vm.CompileMode;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jcstress.infra.TestInfo;
import org.openjdk.jcstress.infra.runners.Runner;
import org.openjdk.jcstress.infra.runners.TestConfig;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.LinuxPerfAsmProfiler;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@State(Scope.Thread)
public class SampleTestBench {

    // Must match the source code.
    private static final List<String> ACTOR_NAMES = Arrays.asList("actor1", "actor2");;

    private static final TestConfig[] CFGS;
    private static final Constructor<?> CNSTR;

    static {
        try {
            Options opts = new Options(new String[] {
                    "-v",
                    "-iters", "1",
                    "-time", "10000"});
            opts.parse();

            String testName = SampleTest.class.getCanonicalName();
            String runnerName = SampleTest_jcstress.class.getCanonicalName();

            TestInfo ti = new TestInfo(testName, testName, runnerName, "", 2, ACTOR_NAMES, false);
            SchedulingClass sc = new SchedulingClass(AffinityMode.NONE, 2, NodeType.PACKAGE);

            int[] casesFor = CompileMode.casesFor(2, true, true);
            casesFor = Arrays.copyOf(casesFor, casesFor.length + 1);
            casesFor[casesFor.length - 1] = CompileMode.UNIFIED;

            CFGS = new TestConfig[casesFor.length];
            for (int i = 0; i < casesFor.length; i++) {
                CFGS[i] = new TestConfig(opts, ti, 1, Collections.emptyList(), casesFor[i], sc);
            }

            Class<?> aClass = Class.forName(runnerName);
            CNSTR = aClass.getConstructor(ForkedTestConfig.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Runner<?> o;

    @Setup
    public void setup() throws Throwable {
        o = (Runner<?>) CNSTR.newInstance(new ForkedTestConfig(CFGS[0]));
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Fork(1)
    public void testMethod() {
        o.run();
    }

    public static void main(String... args) throws IOException, RunnerException {
        for (TestConfig cfg : CFGS) {
            File cdFile = null;
            try {
                cdFile = File.createTempFile("jcstress", "directives");

                PrintWriter pw = new PrintWriter(cdFile);
                cfg.generateDirectives(pw, new Verbosity(1));
                pw.close();

                org.openjdk.jmh.runner.options.Options opts = new OptionsBuilder()
                        .include(SampleTestBench.class.getName())
                        .jvmArgsPrepend("-XX:+UnlockDiagnosticVMOptions", "-XX:CompilerDirectivesFile=" + cdFile.getAbsolutePath())
                        .addProfiler(LinuxPerfAsmProfiler.class, "hotThreshold=0.05")
                        .build();

                System.out.println();
                System.out.println("--------------------------------------------------------------------------------------------------");
                System.out.println(CompileMode.description(cfg.getCompileMode(), ACTOR_NAMES));
                System.out.println();
                cfg.generateDirectives(new PrintWriter(System.out, true), new Verbosity(1));
                System.out.println();

                new org.openjdk.jmh.runner.Runner(opts).runSingle();
            } finally {
                if (cdFile != null) {
                    boolean succ = cdFile.delete();
                    if (!succ) {
                        System.out.println("WARNING: Cannot delete compiler directives file");
                    }
                }
            }
        }
    }

}
