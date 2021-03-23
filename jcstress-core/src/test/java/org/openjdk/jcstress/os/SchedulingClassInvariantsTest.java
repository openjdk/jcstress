package org.openjdk.jcstress.os;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openjdk.jcstress.os.topology.PresetTopology;
import org.openjdk.jcstress.os.topology.Topology;
import org.openjdk.jcstress.os.topology.TopologyParseException;

import java.util.*;

@RunWith(Parameterized.class)
public class SchedulingClassInvariantsTest {

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
        Scheduler s = new Scheduler(topo, topo.totalThreads());
        for (int a = 1; a <= 4; a++) {
            checkInvariants(s.localAffinityFor(a, topo.totalThreads()));
            checkInvariants(s.globalAffinityFor(a, topo.totalThreads()));
            checkInvariants(s.noneAffinityFor(a, topo.totalThreads()));
        }
    }

    private static void checkInvariants(List<SchedulingClass> scls) {
        for (SchedulingClass scl : scls) {
            for (int ca : scl.coreActors()) {
                Assert.assertNotEquals("Core classes should be consecutive: " + scl, 0, ca);
            }
            for (int pa : scl.packageActors()) {
                Assert.assertNotEquals("Package classes should be consecutive: " + scl, 0, pa);
            }

            for (int a1 = 0; a1 < scl.numActors(); a1++) {
                for (int a2 = 0; a2 < scl.numActors(); a2++) {
                    if (scl.packages[a1] != scl.packages[a2]) {
                        Assert.assertNotEquals("Different packages should yield different core classes: " + scl,
                                scl.cores[a1], scl.cores[a2]);
                    }
                }
            }
        }
    }

}
