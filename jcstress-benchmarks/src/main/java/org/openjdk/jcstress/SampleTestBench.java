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

import org.openjdk.jcstress.os.AffinityMode;
import org.openjdk.jcstress.os.CPUMap;
import org.openjdk.jcstress.os.SchedulingClass;
import org.openjdk.jcstress.vm.CompileMode;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jcstress.infra.TestInfo;
import org.openjdk.jcstress.infra.collectors.TestResultCollector;
import org.openjdk.jcstress.infra.grading.ConsoleReportPrinter;
import org.openjdk.jcstress.infra.runners.Runner;
import org.openjdk.jcstress.infra.runners.TestConfig;
import org.openjdk.jmh.annotations.*;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@State(Scope.Thread)
public class SampleTestBench {

    private Runner<?> o;
    private ExecutorService pool;

    @Setup
    public void setup() throws Throwable {
        Options opts = new Options(new String[]{"-v", "-iters", "1", "-time", "5000", "-deoptMode", "NONE"});
        opts.parse();
        PrintWriter pw = new PrintWriter(System.out, true);
        ConsoleReportPrinter sink = new ConsoleReportPrinter(opts, pw, 1);

        String testName = SampleTest.class.getCanonicalName();
        String runnerName = SampleTest_jcstress.class.getCanonicalName();

        TestInfo ti = new TestInfo(testName, runnerName, "", 2, Arrays.asList("a1", "a2"), false);
        TestConfig cfg = new TestConfig(opts, ti, 1, Collections.emptyList(), CompileMode.UNIFIED, new SchedulingClass(AffinityMode.NONE, 2));
        int[] map = new int[2];
        map[0] = -1;
        map[1] = -1;
        cfg.setCPUMap(new CPUMap(map, map, map, map));

        pool = Executors.newCachedThreadPool();

        Class<?> aClass = Class.forName(runnerName);
        Constructor<?> cnstr = aClass.getConstructor(TestConfig.class, TestResultCollector.class, ExecutorService.class);
        o = (Runner<?>) cnstr.newInstance(cfg, sink, pool);
    }

    @TearDown
    public void tearDown() {
        pool.shutdown();
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void testMethod() throws Throwable {
        o.run();
    }

}
