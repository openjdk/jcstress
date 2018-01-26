/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress.infra.runners;

import org.openjdk.jcstress.Options;
import org.openjdk.jcstress.infra.TestInfo;
import org.openjdk.jcstress.vm.AllocProfileSupport;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class TestConfig implements Serializable {

    public final SpinLoopStyle spinLoopStyle;
    public final boolean verbose;
    public final int time;
    public final int iters;
    public final int deoptRatio;
    public final int threads;
    public final String name;
    public final String generatedRunnerName;
    public final List<String> jvmArgs;
    public final RunMode runMode;
    public final int forkId;
    public final int maxFootprintMB;
    public int minStride;
    public int maxStride;
    public StrideCap strideCap;

    public enum RunMode {
        EMBEDDED,
        FORKED,
    }

    public enum StrideCap {
        NONE,
        FOOTPRINT,
        TIME,
    }

    public TestConfig(Options opts, TestInfo info, RunMode runMode, int forkId, List<String> jvmArgs) {
        this.runMode = runMode;
        this.forkId = forkId;
        this.jvmArgs = jvmArgs;
        time = opts.getTime();
        minStride = opts.getMinStride();
        maxStride = opts.getMaxStride();
        iters = opts.getIterations();
        spinLoopStyle = opts.getSpinStyle();
        verbose = opts.isVerbose();
        deoptRatio = opts.deoptRatio();
        maxFootprintMB = opts.getMaxFootprintMb();
        threads = info.threads();
        name = info.name();
        generatedRunnerName = info.generatedRunner();
        strideCap = StrideCap.NONE;
    }

    public void adjustStrides(Consumer<Integer> tryAllocate) {
        int count = 1;
        int succCount = count;
        while (true) {
            StrideCap cap = tryWith(tryAllocate, count);
            if (cap != StrideCap.NONE) {
                strideCap = cap;
                break;
            }

            // success!
            succCount = count;

            // do not go over the maxStride
            if (succCount > maxStride) {
                succCount = maxStride;
                break;
            }

            count *= 2;
        }

        maxStride = Math.min(maxStride, succCount);
        minStride = Math.min(minStride, succCount);
    }

    private StrideCap tryWith(Consumer<Integer> tryAllocate, int count) {
        final int TRIES = 10;
        for (int tries = 0; tries < TRIES; tries++) {
            long startFoot = AllocProfileSupport.getAllocatedBytes();
            long startTime = System.nanoTime();
            try {
                tryAllocate.accept(count);
                long usedTime = System.nanoTime() - startTime;
                long footprint = AllocProfileSupport.getAllocatedBytes() - startFoot;

                if (footprint > maxFootprintMB * 1024 * 1024) {
                    // blown the footprint estimate
                    return StrideCap.FOOTPRINT;
                }

                if (TimeUnit.NANOSECONDS.toMillis(usedTime) > time) {
                    // blown the time estimate
                    return StrideCap.TIME;
                }

            } catch (OutOfMemoryError err) {
                // blown the heap size
                return StrideCap.FOOTPRINT;
            }
        }
        return StrideCap.NONE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestConfig that = (TestConfig) o;

        if (spinLoopStyle != that.spinLoopStyle) return false;
        if (minStride != that.minStride) return false;
        if (maxStride != that.maxStride) return false;
        if (time != that.time) return false;
        if (iters != that.iters) return false;
        if (deoptRatio != that.deoptRatio) return false;
        if (threads != that.threads) return false;
        if (!name.equals(that.name)) return false;
        if (!jvmArgs.equals(that.jvmArgs)) return false;
        return runMode == that.runMode;

    }

    @Override
    public int hashCode() {
        int result = spinLoopStyle.hashCode();
        result = 31 * result + minStride;
        result = 31 * result + maxStride;
        result = 31 * result + time;
        result = 31 * result + iters;
        result = 31 * result + deoptRatio;
        result = 31 * result + threads;
        result = 31 * result + name.hashCode();
        result = 31 * result + jvmArgs.hashCode();
        result = 31 * result + runMode.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "JVM options: " + jvmArgs + "\n" +
                "Iterations: " + iters + "\n" +
                "Time: " + time + "\n" +
                "Stride: [" + minStride + ", " + maxStride + "] (capped by " + strideCap + ")";
    }
}
