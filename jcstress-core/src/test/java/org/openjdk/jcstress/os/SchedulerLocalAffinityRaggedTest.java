/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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

import org.junit.Test;
import org.openjdk.jcstress.os.topology.PresetListTopology;
import org.openjdk.jcstress.os.topology.TopologyParseException;

@Execution(CONCURRENT)
public class SchedulerLocalAffinityRaggedTest extends AbstractSchedulerAffinityTest {

    @Test
    public void test_unevenCores() throws TopologyParseException {
        PresetListTopology topo = new PresetListTopology();
        topo.add(0, 0, 0);
        topo.add(0, 1, 1);
        topo.add(0, 1, 2);
        topo.add(0, 1, 3);
        topo.finish();

        int maxThreads = topo.totalThreads();
        Scheduler s = new Scheduler(topo, maxThreads);
        s.enableDebug();

        runLocal(topo, s, maxThreads);
    }

    @Test
    public void test_unevenPackages() throws TopologyParseException {
        PresetListTopology topo = new PresetListTopology();
        topo.add(0, 0, 0);
        topo.add(0, 1, 1);
        topo.add(1, 2, 2);
        topo.add(1, 3, 3);
        topo.add(1, 4, 4);
        topo.finish();

        int maxThreads = topo.totalThreads();
        Scheduler s = new Scheduler(topo, maxThreads);
        s.enableDebug();

        runLocal(topo, s, maxThreads);
    }

}
