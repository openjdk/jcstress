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

import java.io.Serializable;
import java.util.List;

public class CPUMap implements Serializable {
    private final int[] actorThreads;
    private final int[] systemThreads;
    private final int[] threadToPackage;
    private final int[] threadToCore;
    private final int[] allocatedThreads;
    private final int[] threadToRealCPU;

    public CPUMap(int[] allocatedThreads, int[] actorThreads, int[] systemThreads,
                  int[] threadToPackage, int[] threadToCore, int[] threadToRealCPU) {
        this.allocatedThreads = allocatedThreads;
        this.actorThreads = actorThreads;
        this.systemThreads = systemThreads;
        this.threadToPackage = threadToPackage;
        this.threadToCore = threadToCore;
        this.threadToRealCPU = threadToRealCPU;
    }

    public int[] allocatedThreads() {
        return allocatedThreads;
    }

    public int[] actorThreads() {
        return actorThreads;
    }

    public int[] actorRealCPUs() {
        int[] r = new int[actorThreads.length];
        for (int i = 0; i < actorThreads.length; i++) {
            r[i] = threadToRealCPU[actorThreads[i]];
        }
        return r;
    }

    public int[] systemThreads() {
        return systemThreads;
    }

    public static String description(CPUMap map, List<String> actorNames) {
        int[] actorToThread = map.actorThreads;
        int[] systemMap = map.systemThreads;
        int[] packageMap = map.threadToPackage;
        int[] coreMap = map.threadToCore;
        int[] threadToRealCPU = map.threadToRealCPU;

        boolean hasOne = false;

        StringBuilder sb = new StringBuilder();
        for (int a = 0; a < actorToThread.length; a++) {
            if (actorToThread[a] != -1) {
                if (!hasOne) {
                    sb.append("\n");
                    hasOne = true;
                }
                sb.append("    ");
                sb.append(actorNames.get(a));
                sb.append(": CPU #");
                sb.append(threadToRealCPU[actorToThread[a]]);
                sb.append(" (thread #");
                sb.append(actorToThread[a]);
                sb.append(", core #");
                sb.append(coreMap[actorToThread[a]]);
                sb.append(", node/package #");
                sb.append(packageMap[actorToThread[a]]);
                sb.append(")");
                sb.append(System.lineSeparator());
            }
        }
        for (int a = 0; a < systemMap.length; a++) {
            if (systemMap[a] != -1) {
                if (!hasOne) {
                    sb.append("\n");
                    hasOne = true;
                }
                sb.append("    <system>: CPU #");
                sb.append(threadToRealCPU[systemMap[a]]);
                sb.append(" (thread #");
                sb.append(systemMap[a]);
                sb.append(", core #");
                sb.append(coreMap[systemMap[a]]);
                sb.append(", node/package #");
                sb.append(packageMap[systemMap[a]]);
                sb.append(")");
                sb.append(System.lineSeparator());
            }
        }
        if (!hasOne) {
            sb.append("unspecified");
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }

    public String globalAffinityMap() {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for (int a : actorThreads) {
            if (a == -1) continue;
            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }
            sb.append(threadToRealCPU[a]);
        }

        for (int a : systemThreads) {
            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }
            sb.append(threadToRealCPU[a]);
        }

        return sb.toString();
    }
}
