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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openjdk.jcstress.os.topology.PresetTopology;
import org.openjdk.jcstress.os.topology.Topology;
import org.openjdk.jcstress.os.topology.TopologyParseException;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

@RunWith(Parameterized.class)
public class SchedulerGlobalAffinityTest {

    @Parameterized.Parameters(name = "p={0} c={1} t={2} limited={3}")
    public static Iterable<Object[]> data() {
        List<Object[]> r = new ArrayList<>();
        for (int p = 1; p <= 4; p++) {
            for (int c : new int[] { 1, 2, 4, 5, 6, 8 }) {
                for (int t : new int[] { 1, 2, 8 }) {
                    r.add(new Object[] { p, c, t, false });
                    r.add(new Object[] { p, c, t, true });
                }
            }
        }
        return r;
    }

    @Parameterized.Parameter(0)
    public int p;

    @Parameterized.Parameter(1)
    public int c;

    @Parameterized.Parameter(2)
    public int t;

    @Parameterized.Parameter(3)
    public boolean limited;

    @Test
    public void test() throws TopologyParseException {
        Topology topo = new PresetTopology(p, c, t);
        int maxThreads = limited ? Math.min(4, topo.totalCores()) : topo.totalCores();
        Scheduler s = new Scheduler(topo, maxThreads);
        s.enableDebug();

        Queue<CPUMap> takenMaps = new LinkedBlockingQueue<>();

        List<SchedulingClass> cases = new ArrayList<>();
        for (int a = 1; a <= 4; a++) {
            List<SchedulingClass> skel = s.globalAffinityFor(a, maxThreads);
            for (int c = 0; c < 1000; c++) {
                cases.addAll(skel);
            }
        }

        Collections.shuffle(cases, new Random(12345));

        for (SchedulingClass scl : cases) {
            CPUMap cpuMap = s.tryAcquire(scl);
            while (cpuMap == null) {
                CPUMap old = takenMaps.poll();
                Assert.assertNotNull("Cannot schedule on empty system", old);
                s.release(old);
                cpuMap = s.tryAcquire(scl);
            }

            takenMaps.offer(cpuMap);

            Assert.assertEquals(scl.numActors(), cpuMap.actorMap().length);
            for (int c : cpuMap.actorMap()) {
                Assert.assertEquals(-1, c);
            }
            Assert.assertNotEquals(0, cpuMap.systemMap().length);
        }
    }


}
