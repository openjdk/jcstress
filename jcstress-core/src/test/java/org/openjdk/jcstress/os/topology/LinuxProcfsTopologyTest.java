package org.openjdk.jcstress.os.topology;

import org.junit.*;
import org.openjdk.jcstress.util.FileUtils;
import org.openjdk.jcstress.vm.VMSupport;

import java.io.IOException;

public class LinuxProcfsTopologyTest extends AbstractTopologyTest {

    @Before
    public void preconditions() {
        Assume.assumeTrue(VMSupport.isLinux());
    }

    @Test
    @Ignore // Comment this to test that current environment is parsable.
    public void test_Current() throws TopologyParseException {
        new LinuxProcfsTopology();
    }

    @Test
    public void test_Saved_1() throws TopologyParseException, IOException {
        String s = FileUtils.copyFileToTemp("/topology/cpuinfo-1.txt", "jcstress", "test");
        LinuxProcfsTopology topo = new LinuxProcfsTopology(s);

        Assert.assertEquals(1,  topo.packagesPerSystem());
        Assert.assertEquals(32, topo.coresPerPackage());
        Assert.assertEquals(2,  topo.threadsPerCore());
        Assert.assertEquals(32, topo.totalCores());
        Assert.assertEquals(64, topo.totalThreads());

        for (int t = 0; t < topo.totalThreads(); t++) {
            Assert.assertEquals(0, topo.threadToPackage(t));
            Assert.assertEquals(t % topo.coresPerPackage(), topo.threadToCore(t));
        }

        checkGenericInvariants(topo);
    }

    @Test
    public void test_Saved_2() throws IOException {
        String s = FileUtils.copyFileToTemp("/topology/cpuinfo-2.txt", "jcstress", "test");

        try {
            new LinuxProcfsTopology(s);
            Assert.fail("Should have failed");
        } catch (TopologyParseException topo) {
            // Should fail
        }
    }

    @Test
    public void test_Saved_3() throws IOException {
        String s = FileUtils.copyFileToTemp("/topology/cpuinfo-3.txt", "jcstress", "test");

        try {
            new LinuxProcfsTopology(s);
            Assert.fail("Should have failed");
        } catch (TopologyParseException topo) {
            // Should fail
        }
    }

    @Test
    public void test_Saved_4() throws IOException {
        String s = FileUtils.copyFileToTemp("/topology/cpuinfo-4.txt", "jcstress", "test");

        try {
            new LinuxProcfsTopology(s);
            Assert.fail("Should have failed");
        } catch (TopologyParseException topo) {
            // Should fail
        }
    }

    @Test
    public void test_Saved_5() throws IOException {
        String s = FileUtils.copyFileToTemp("/topology/cpuinfo-5.txt", "jcstress", "test");

        try {
            new LinuxProcfsTopology(s);
            Assert.fail("Should have failed");
        } catch (TopologyParseException topo) {
            // Should fail
        }
    }

    @Test
    public void test_Saved_6() throws IOException {
        String s = FileUtils.copyFileToTemp("/topology/cpuinfo-6.txt", "jcstress", "test");

        try {
            new LinuxProcfsTopology(s);
            Assert.fail("Should have failed");
        } catch (TopologyParseException topo) {
            // Should fail
        }
    }

    @Test
    public void test_Saved_7() throws IOException {
        String s = FileUtils.copyFileToTemp("/topology/cpuinfo-7.txt", "jcstress", "test");

        try {
            new LinuxProcfsTopology(s);
            Assert.fail("Should have failed");
        } catch (TopologyParseException topo) {
            // Should fail
        }
    }

}
