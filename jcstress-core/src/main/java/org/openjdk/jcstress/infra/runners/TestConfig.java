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

import org.openjdk.jcstress.os.SchedulingClass;
import org.openjdk.jcstress.Options;
import org.openjdk.jcstress.infra.TestInfo;
import org.openjdk.jcstress.os.CPUMap;

import java.io.Serializable;
import java.util.List;

public class TestConfig implements Serializable {
    public final SpinLoopStyle spinLoopStyle;
    public final int time;
    public final int iters;
    public final int threads;
    public final String name;
    public final String binaryName;
    public final String generatedRunnerName;
    public final List<String> jvmArgs;
    public final int forkId;
    public final int maxFootprintMB;
    public final List<String> actorNames;
    public final int compileMode;
    public final SchedulingClass shClass;
    public final int strideSize;
    public int strideCount;
    public CPUMap cpuMap;

    public void setCPUMap(CPUMap cpuMap) {
        this.cpuMap = cpuMap;
    }

    public TestConfig(Options opts, TestInfo info, int forkId, List<String> jvmArgs, int compileMode, SchedulingClass scl) {
        this.forkId = forkId;
        this.jvmArgs = jvmArgs;
        time = opts.getTime();
        strideSize = opts.getStrideSize();
        strideCount = opts.getStrideCount();
        iters = opts.getIterations();
        spinLoopStyle = opts.getSpinStyle();
        maxFootprintMB = opts.getMaxFootprintMb();
        threads = info.threads();
        name = info.name();
        binaryName = info.binaryName();
        generatedRunnerName = info.generatedRunner();
        actorNames = info.actorNames();
        this.compileMode = compileMode;
        shClass = scl;
    }

    public int getCompileMode() {
        return compileMode;
    }

    public SchedulingClass getSchedulingClass() {
        return shClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestConfig that = (TestConfig) o;

        if (!name.equals(that.name)) return false;
        if (spinLoopStyle != that.spinLoopStyle) return false;
        if (strideSize != that.strideSize) return false;
        if (strideCount != that.strideCount) return false;
        if (time != that.time) return false;
        if (iters != that.iters) return false;
        if (threads != that.threads) return false;
        if (compileMode != that.compileMode) return false;
        if (!jvmArgs.equals(that.jvmArgs)) return false;
        if (!shClass.equals(that.shClass)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}
