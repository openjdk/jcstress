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

import org.openjdk.jcstress.os.AffinityMode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ForkedTestConfig {
    public final SpinLoopStyle spinLoopStyle;
    public final int time;
    public final int iters;
    public final String generatedRunnerName;
    public final int maxFootprintMB;
    public int strideSize;
    public int strideCount;
    public boolean localAffinity;
    public int[] localAffinityMap;

    public ForkedTestConfig(TestConfig cfg) {
        spinLoopStyle = cfg.spinLoopStyle;
        time = cfg.time;
        iters = cfg.iters;
        generatedRunnerName = cfg.generatedRunnerName;
        maxFootprintMB = cfg.maxFootprintMB;
        strideSize = cfg.strideSize;
        strideCount = cfg.strideCount;
        localAffinity = cfg.shClass.mode() == AffinityMode.LOCAL;
        if (localAffinity) {
            localAffinityMap = cfg.cpuMap.actorMap();
        }
    }

    public ForkedTestConfig(DataInputStream dis) throws IOException {
        spinLoopStyle = SpinLoopStyle.values()[dis.readInt()];
        time = dis.readInt();
        iters = dis.readInt();
        generatedRunnerName = dis.readUTF();
        maxFootprintMB = dis.readInt();
        strideSize = dis.readInt();
        strideCount = dis.readInt();
        localAffinity = dis.readBoolean();
        if (localAffinity) {
            int len = dis.readInt();
            localAffinityMap = new int[len];
            for (int c = 0; c < len; c++) {
                localAffinityMap[c] = dis.readInt();
            }
        }
    }

    public void write(DataOutputStream dos) throws IOException {
        dos.writeInt(spinLoopStyle.ordinal());
        dos.writeInt(time);
        dos.writeInt(iters);
        dos.writeUTF(generatedRunnerName);
        dos.writeInt(maxFootprintMB);
        dos.writeInt(strideSize);
        dos.writeInt(strideCount);
        dos.writeBoolean(localAffinity);
        if (localAffinity) {
            dos.writeInt(localAffinityMap.length);
            for (int am : localAffinityMap) {
                dos.writeInt(am);
            }
        }
    }

    public void adjustStrideCount(FootprintEstimator estimator) {
        int count = 1;
        int succCount = count;
        while (tryWith(estimator, count)) {
            // success!
            succCount = count;

            // do not go over the the limit
            if (succCount >= strideSize * strideCount) {
                succCount = strideSize * strideCount;
                break;
            }

            count *= 2;
        }

        strideSize = Math.min(succCount, strideSize);
        strideCount = succCount / strideSize;
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
