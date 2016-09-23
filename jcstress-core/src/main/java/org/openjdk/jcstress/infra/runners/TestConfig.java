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
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class TestConfig implements Serializable {

    public static final Comparator<TestConfig> COMPARATOR_NAME = Comparator.comparing((c) -> c.name);

    public final int uniqueToken;
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

    public enum RunMode {
        EMBEDDED,
        FORKED,
    }

    public TestConfig(int uniqueToken, Options opts, TestInfo info, RunMode runMode, int forkId, List<String> jvmArgs) {
        this.uniqueToken = uniqueToken;
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
    }

    public void adjustStrides(Consumer<Integer> tryAllocate) {
        int count = 1;
        int succCount = count;
        while (true) {
            long start = AllocProfileSupport.getAllocatedBytes();
            try {
                tryAllocate.accept(count);
                long footprint = AllocProfileSupport.getAllocatedBytes() - start;

                if (footprint > maxFootprintMB * 1024 * 1024) {
                    // blown the footprint estimate
                    break;
                }
            } catch (OutOfMemoryError err) {
                // blown the heap size
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
                "Time: " + time;
    }
}
