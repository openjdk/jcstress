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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class ForkedTestConfig {
    public final SpinLoopStyle spinLoopStyle;
    public final int time;
    public final int iters;
    public final String generatedRunnerName;
    public final int maxFootprintMB;
    public int minStride;
    public int maxStride;
    public int[] actorMap;

    public ForkedTestConfig(TestConfig cfg) {
        spinLoopStyle = cfg.spinLoopStyle;
        time = cfg.time;
        iters = cfg.iters;
        generatedRunnerName = cfg.generatedRunnerName;
        maxFootprintMB = cfg.maxFootprintMB;
        minStride = cfg.minStride;
        maxStride = cfg.maxStride;
        actorMap = cfg.cpuMap.actorMap();
    }

    public ForkedTestConfig(DataInputStream dis) throws IOException {
        spinLoopStyle = SpinLoopStyle.values()[dis.readInt()];
        time = dis.readInt();
        iters = dis.readInt();
        generatedRunnerName = dis.readUTF();
        maxFootprintMB = dis.readInt();
        minStride = dis.readInt();
        maxStride = dis.readInt();
        int len = dis.readInt();
        actorMap = new int[len];
        for (int c = 0; c < len; c++) {
            actorMap[c] = dis.readInt();
        }
    }

    public void write(DataOutputStream dos) throws IOException {
        dos.writeInt(spinLoopStyle.ordinal());
        dos.writeInt(time);
        dos.writeInt(iters);
        dos.writeUTF(generatedRunnerName);
        dos.writeInt(maxFootprintMB);
        dos.writeInt(minStride);
        dos.writeInt(maxStride);
        dos.writeInt(actorMap.length);
        for (int am : actorMap) {
            dos.writeInt(am);
        }
    }

    public void adjustStrides(FootprintEstimator estimator) {
        int count = 1;
        int succCount = count;
        while (true) {
            if (!tryWith(estimator, count)) {
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

    private boolean tryWith(FootprintEstimator estimator, int count) {
        try {
            long[] cnts = new long[2];
            estimator.runWith(count, cnts);
            long footprint = cnts[0];
            long usedTime = cnts[1];

            if (footprint > maxFootprintMB * 1024 * 1024) {
                // blown the footprint estimate
                return false;
            }

            if (TimeUnit.NANOSECONDS.toMillis(usedTime) > time) {
                // blown the time estimate
                return false;
            }

        } catch (OutOfMemoryError err) {
            // blown the heap size
            return false;
        }
        return true;
    }

}
