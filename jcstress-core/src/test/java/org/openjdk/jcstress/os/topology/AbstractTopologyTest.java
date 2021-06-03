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

import java.util.Collection;

public class AbstractTopologyTest {

    protected void checkGenericInvariants(Topology topo) {
        Assert.assertEquals(topo.packagesPerSystem() * topo.coresPerPackage() * topo.threadsPerCore(), topo.totalThreads());

        for (int c = 0; c < topo.totalCores(); c++) {
            Collection<Integer> coreThreads = topo.coreThreads(c);
            Assert.assertEquals(topo.threadsPerCore(), coreThreads.size());
            for (int t : coreThreads) {
                Assert.assertEquals(c, topo.threadToCore(t));
            }
        }

        for (int p = 0; p < topo.packagesPerSystem(); p++) {
            Collection<Integer> packageCores = topo.packageCores(p);
            Assert.assertEquals(topo.coresPerPackage(), packageCores.size());
            for (int c : packageCores) {
                Assert.assertEquals(p, topo.coreToPackage(c));
                Collection<Integer> coreThreads = topo.coreThreads(c);
                Assert.assertEquals(topo.threadsPerCore(), coreThreads.size());
                for (int t : coreThreads) {
                    Assert.assertEquals(c, topo.threadToCore(t));
                    Assert.assertEquals(p, topo.threadToPackage(t));
                }
            }
        }
    }


}
