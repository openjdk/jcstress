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
