package org.openjdk.jcstress.os.topology;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.openjdk.jcstress.vm.VMSupport;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class LinuxSysfsTopologyTest extends AbstractTopologyTest {

    @Before
    public void preconditions() {
        Assume.assumeTrue(VMSupport.isLinux());
    }

    @Test
    public void test_Current() throws TopologyParseException {
        // Verifies the current is parsable
        new LinuxSysfsTopology();
    }

    /*
       Saved sysfs snapshots are created on target systems with:
         $ find /sys/devices/system/cpu/ -type f -path *topology* -printf "%P: " -exec cat {} \;
     */

    @Test
    public void test_Saved_1() throws TopologyParseException, IOException {
        FileSystem fs = parse("/topology/sysfs-1.txt");
        LinuxSysfsTopology topo = new LinuxSysfsTopology(fs.getPath(""));

        Assert.assertEquals(1,  topo.packagesPerSystem());
        Assert.assertEquals(32, topo.coresPerPackage());
        Assert.assertEquals(2,  topo.threadsPerCore());
        Assert.assertEquals(32, topo.totalCores());
        Assert.assertEquals(64, topo.totalThreads());

        for (int t = 0; t < topo.totalThreads(); t++) {
            Assert.assertEquals(0, topo.threadToPackage(t));
            Assert.assertEquals(t % 32, topo.threadToCore(t));
        }

        checkGenericInvariants(topo);
    }

    @Test
    public void test_Saved_2() throws TopologyParseException, IOException {
        FileSystem fs = parse("/topology/sysfs-2.txt");
        LinuxSysfsTopology topo = new LinuxSysfsTopology(fs.getPath(""));

        Assert.assertEquals(1, topo.packagesPerSystem());
        Assert.assertEquals(4, topo.coresPerPackage());
        Assert.assertEquals(1, topo.threadsPerCore());
        Assert.assertEquals(4, topo.totalCores());
        Assert.assertEquals(4, topo.totalThreads());

        for (int t = 0; t < topo.totalThreads(); t++) {
            Assert.assertEquals(0, topo.threadToPackage(t));
            Assert.assertEquals(t, topo.threadToCore(t));
        }

        checkGenericInvariants(topo);
    }

    @Test
    public void test_Saved_3() throws TopologyParseException, IOException {
        FileSystem fs = parse("/topology/sysfs-3.txt");
        LinuxSysfsTopology topo = new LinuxSysfsTopology(fs.getPath(""));

        Assert.assertEquals(2,  topo.packagesPerSystem());
        Assert.assertEquals(5,  topo.coresPerPackage());
        Assert.assertEquals(8,  topo.threadsPerCore());
        Assert.assertEquals(10, topo.totalCores());
        Assert.assertEquals(80, topo.totalThreads());

        for (int t = 0; t < topo.totalThreads(); t++) {
            Assert.assertEquals(t / 40, topo.threadToPackage(t));
            Assert.assertEquals(t / 8, topo.threadToCore(t));
        }

        checkGenericInvariants(topo);
    }

    @Test
    public void test_Saved_4() throws TopologyParseException, IOException {
        FileSystem fs = parse("/topology/sysfs-4.txt");
        LinuxSysfsTopology topo = new LinuxSysfsTopology(fs.getPath(""));

        Assert.assertEquals(10, topo.packagesPerSystem());
        Assert.assertEquals(1,  topo.coresPerPackage());
        Assert.assertEquals(8,  topo.threadsPerCore());
        Assert.assertEquals(10, topo.totalCores());
        Assert.assertEquals(80, topo.totalThreads());

        for (int t = 0; t < topo.totalThreads(); t++) {
            Assert.assertEquals(t / 8, topo.threadToPackage(t));
            Assert.assertEquals(t / 8, topo.threadToCore(t));
        }

        checkGenericInvariants(topo);
    }

    @Test
    public void test_Saved_5() throws TopologyParseException, IOException {
        FileSystem fs = parse("/topology/sysfs-5.txt");
        LinuxSysfsTopology topo = new LinuxSysfsTopology(fs.getPath(""));

        Assert.assertEquals(1,  topo.packagesPerSystem());
        Assert.assertEquals(64, topo.coresPerPackage());
        Assert.assertEquals(1,  topo.threadsPerCore());
        Assert.assertEquals(64, topo.totalCores());
        Assert.assertEquals(64, topo.totalThreads());

        for (int t = 0; t < topo.totalThreads(); t++) {
            Assert.assertEquals(0, topo.threadToPackage(t));
            Assert.assertEquals(t, topo.threadToCore(t));
        }

        checkGenericInvariants(topo);
    }

    @Test
    public void test_Saved_6() throws TopologyParseException, IOException {
        FileSystem fs = parse("/topology/sysfs-6.txt");
        LinuxSysfsTopology topo = new LinuxSysfsTopology(fs.getPath(""));

        Assert.assertEquals(2,  topo.packagesPerSystem());
        Assert.assertEquals(1, topo.coresPerPackage());
        Assert.assertEquals(1,  topo.threadsPerCore());
        Assert.assertEquals(2, topo.totalCores());
        Assert.assertEquals(2, topo.totalThreads());

        for (int t = 0; t < topo.totalThreads(); t++) {
            Assert.assertEquals(t, topo.threadToPackage(t));
            Assert.assertEquals(t, topo.threadToCore(t));
        }

        checkGenericInvariants(topo);
    }

    @Test
    public void test_Saved_7() throws TopologyParseException, IOException {
        FileSystem fs = parse("/topology/sysfs-7.txt");
        LinuxSysfsTopology topo = new LinuxSysfsTopology(fs.getPath(""));

        Assert.assertEquals(2, topo.packagesPerSystem());
        Assert.assertEquals(4, topo.coresPerPackage());
        Assert.assertEquals(1, topo.threadsPerCore());
        Assert.assertEquals(8, topo.totalCores());
        Assert.assertEquals(8, topo.totalThreads());

        for (int t = 0; t < topo.totalThreads(); t++) {
            Assert.assertEquals(t % 2, topo.threadToPackage(t));
            Assert.assertEquals((t % 2) * 4 + (t / 2), topo.threadToCore(t));
        }

        checkGenericInvariants(topo);
    }

    private FileSystem parse(String resource) throws IOException {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        try (InputStream is = LinuxSysfsTopologyTest.class.getResourceAsStream(resource);
             InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] split = line.split(":");
                String filename = split[0];
                String contents = split[1].substring(1); // trim leading whitespace
                Path path = fs.getPath(filename);
                Files.createDirectories(path.getParent());
                Files.write(path, Collections.singletonList(contents));
            }
        }
        return fs;
    }

}
