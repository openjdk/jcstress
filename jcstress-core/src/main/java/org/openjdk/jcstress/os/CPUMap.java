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
    private final int[] actorMap;
    private final int[] systemMap;
    private final int[] packageMap;
    private final int[] coreMap;
    private final int[] allocatedMap;

    public CPUMap(int[] allocatedMap, int[] actorMap, int[] systemMap, int[] packageMap, int[] coreMap) {
        this.allocatedMap = allocatedMap;
        this.actorMap = actorMap;
        this.systemMap = systemMap;
        this.packageMap = packageMap;
        this.coreMap = coreMap;
    }

    public int[] allocatedMap() {
        return allocatedMap;
    }

    public int[] actorMap() {
        return actorMap;
    }

    public int[] systemMap() {
        return systemMap;
    }

    public static String description(CPUMap map, List<String> actorNames) {
        int[] actorMap = map.actorMap;
        int[] systemMap = map.systemMap;
        int[] packageMap = map.packageMap;
        int[] coreMap = map.coreMap;

        boolean hasOne = false;

        StringBuilder sb = new StringBuilder();
        for (int a = 0; a < actorMap.length; a++) {
            if (actorMap[a] != -1) {
                if (!hasOne) {
                    sb.append("\n");
                    hasOne = true;
                }
                sb.append("    ");
                sb.append(actorNames.get(a));
                sb.append(": ");
                sb.append("CPU #");
                sb.append(actorMap[a]);
                sb.append(", package #");
                sb.append(packageMap[actorMap[a]]);
                sb.append(", core #");
                sb.append(coreMap[actorMap[a]]);
                sb.append(System.lineSeparator());
            }
        }
        for (int a = 0; a < systemMap.length; a++) {
            if (systemMap[a] != -1) {
                if (!hasOne) {
                    sb.append("\n");
                    hasOne = true;
                }
                sb.append("    <system>: ");
                sb.append("CPU #");
                sb.append(systemMap[a]);
                sb.append(", package #");
                sb.append(packageMap[systemMap[a]]);
                sb.append(", core #");
                sb.append(coreMap[systemMap[a]]);
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
        for (int a : actorMap) {
            if (a == -1) continue;
            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }
            sb.append(a);
        }

        for (int a : systemMap) {
            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }
            sb.append(a);
        }

        return sb.toString();
    }
}
