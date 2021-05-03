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

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class ForkedTestConfig implements Serializable {
    public final SpinLoopStyle spinLoopStyle;
    public final int time;
    public final int iters;
    public final String generatedRunnerName;
    public final int maxFootprintMB;
    public int minStride;
    public int maxStride;
    public transient StrideCap strideCap;
    public int[] actorMap;

    public enum StrideCap {
        NONE,
        FOOTPRINT,
        TIME,
    }

    public ForkedTestConfig(TestConfig cfg) {
        spinLoopStyle = cfg.spinLoopStyle;
        time = cfg.time;
        iters = cfg.iters;
        generatedRunnerName = cfg.generatedRunnerName;
        maxFootprintMB = cfg.maxFootprintMB;
        minStride = cfg.minStride;
        maxStride = cfg.minStride;
        actorMap = cfg.cpuMap.actorMap();
    }

    public void adjustStrides(FootprintEstimator estimator) {
        int count = 1;
        int succCount = count;
        while (true) {
            StrideCap cap = tryWith(estimator, count);
            if (cap != StrideCap.NONE) {
                strideCap = cap;
                break;
            }

            // success!
            succCount = count;

            // do not go over the maxStride
            if (succCount >= maxStride) {
                succCount = maxStride;
                break;
            }

            count *= 2;
        }

        maxStride = Math.min(maxStride, succCount);
        minStride = Math.min(minStride, succCount);
    }

    public interface FootprintEstimator {
        void runWith(int size, long[] counters);
    }

    private StrideCap tryWith(FootprintEstimator estimator, int count) {
        try {
            long[] cnts = new long[2];
            estimator.runWith(count, cnts);
            long footprint = cnts[0];
            long usedTime = cnts[1];

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
        return StrideCap.NONE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ForkedTestConfig that = (ForkedTestConfig) o;

        if (!generatedRunnerName.equals(that.generatedRunnerName)) return false;
        if (spinLoopStyle != that.spinLoopStyle) return false;
        if (minStride != that.minStride) return false;
        if (maxStride != that.maxStride) return false;
        if (time != that.time) return false;
        if (iters != that.iters) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return generatedRunnerName.hashCode();
    }

}
