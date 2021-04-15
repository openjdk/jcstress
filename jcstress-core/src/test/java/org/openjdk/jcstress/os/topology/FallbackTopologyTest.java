package org.openjdk.jcstress.os.topology;

import org.junit.Assert;
import org.junit.Test;

public class FallbackTopologyTest extends AbstractTopologyTest {

    @Test
    public void test() throws TopologyParseException {
        Topology topo = new FallbackTopology();

        Assert.assertEquals(1, topo.packagesPerSystem());
        Assert.assertEquals(1, topo.threadsPerCore());

        for (int t = 0; t < topo.totalThreads(); t++) {
            Assert.assertEquals(0, topo.threadToPackage(t));
            Assert.assertEquals(t, topo.threadToCore(t));
        }

        for (int c = 0; c < topo.totalCores(); c++) {
            Assert.assertEquals(0, topo.coreToPackage(c));
        }

        checkGenericInvariants(topo);
    }



}
