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
