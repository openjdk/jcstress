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

import java.io.Serializable;

public class TestConfig implements Serializable, Comparable<TestConfig> {

    public final boolean shouldYield;
    public final boolean verbose;
    public final int minStride;
    public final int maxStride;
    public final int time;
    public final int iters;
    public final int deoptRatio;
    public final int threads;
    public final String name;
    public final String generatedRunnerName;
    public final String appendJvmArgs;
    public final RunMode runMode;

    public enum RunMode {
        EMBEDDED,
        FORKED,
    }

    public TestConfig(Options opts, TestInfo info, RunMode runMode) {
        this.runMode = runMode;
        time = opts.getTime();
        minStride = opts.getMinStride();
        maxStride = opts.getMaxStride();
        iters = opts.getIterations();
        shouldYield = opts.shouldYield();
        verbose = opts.isVerbose();
        deoptRatio = opts.deoptRatio();
        threads = info.threads();
        name = info.name();
        appendJvmArgs = opts.getAppendJvmArgs();
        generatedRunnerName = info.generatedRunner();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestConfig that = (TestConfig) o;

        if (shouldYield != that.shouldYield) return false;
        if (verbose != that.verbose) return false;
        if (minStride != that.minStride) return false;
        if (maxStride != that.maxStride) return false;
        if (time != that.time) return false;
        if (iters != that.iters) return false;
        if (deoptRatio != that.deoptRatio) return false;
        if (threads != that.threads) return false;
        if (!name.equals(that.name)) return false;
        if (!generatedRunnerName.equals(that.generatedRunnerName)) return false;
        return appendJvmArgs.equals(that.appendJvmArgs);

    }

    @Override
    public int hashCode() {
        int result = (shouldYield ? 1 : 0);
        result = 31 * result + (verbose ? 1 : 0);
        result = 31 * result + minStride;
        result = 31 * result + maxStride;
        result = 31 * result + time;
        result = 31 * result + iters;
        result = 31 * result + deoptRatio;
        result = 31 * result + threads;
        result = 31 * result + name.hashCode();
        result = 31 * result + generatedRunnerName.hashCode();
        result = 31 * result + appendJvmArgs.hashCode();
        return result;
    }

    @Override
    public int compareTo(TestConfig o) {
        return name.compareTo(o.name);
    }
}
