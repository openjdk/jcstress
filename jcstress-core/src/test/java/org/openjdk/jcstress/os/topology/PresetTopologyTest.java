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
package org.openjdk.jcstress.os.topology;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.PrintStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@RunWith(Parameterized.class)
public class PresetTopologyTest extends AbstractTopologyTest {

    @Parameterized.Parameters(name = "p={0} c={1} t={2}")
    public static Iterable<Object[]> data() {
        List<Object[]> r = new ArrayList<>();
        for (int p = 1; p <= 4; p++) {
            for (int c = 1; c <= 64; c++) {
                for (int t = 1; t <= 8; t++) {
                    r.add(new Object[] { p, c, t });
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

    @Test
    public void test() throws TopologyParseException {
        Topology topo = new PresetTopology(p, c, t);

        Assert.assertEquals(p, topo.packagesPerSystem());
        Assert.assertEquals(c, topo.coresPerPackage());
        Assert.assertEquals(t, topo.threadsPerCore());

        for (int t = 0; t < topo.totalThreads(); t++) {
            Assert.assertEquals(t % topo.totalCores(), topo.threadToCore(t));
        }

        for (int c = 0; c < topo.totalCores(); c++) {
            Assert.assertEquals(c / topo.coresPerPackage(), topo.coreToPackage(c));
        }

        checkGenericInvariants(topo);
    }

}
