package org.openjdk.jcstress.os;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jcstress.os.topology.*;

public class SchedulerTest {

    @Test
    public void trivial_differentCores() throws TopologyParseException {
        Topology t = new PresetTopology(2, 4, 4);
        Scheduler s = new Scheduler(t, t.totalThreads());

        SchedulingClass scl = new SchedulingClass(AffinityMode.LOCAL, 2);
        scl.setPackage(0, 0);
        scl.setPackage(1, 0);
        scl.setCore(0, 0);
        scl.setCore(1, 1);

        CPUMap cpuMap = s.tryAcquire(scl);
        Assert.assertNotNull("Should be scheduled", cpuMap);
        int[] schedule = cpuMap.actorMap();

        Assert.assertTrue("Should be scheduled at the different cores",
                t.threadToCore(schedule[0]) != t.threadToCore(schedule[1]));
    }

    @Test
    public void trivial_sameCore() throws TopologyParseException {
        Topology t = new PresetTopology(2, 4, 4);
        Scheduler s = new Scheduler(t, t.totalThreads());

        SchedulingClass scl = new SchedulingClass(AffinityMode.LOCAL, 2);
        scl.setPackage(0, 0);
        scl.setPackage(1, 0);
        scl.setCore(0, 0);
        scl.setCore(1, 0);

        CPUMap cpuMap = s.tryAcquire(scl);
        Assert.assertNotNull("Should be scheduled", cpuMap);
        int[] schedule = cpuMap.actorMap();

        Assert.assertTrue("Should be scheduled at the same core",
                t.threadToCore(schedule[0]) == t.threadToCore(schedule[1]));
    }

    @Test
    public void trivial_differentPackages() throws TopologyParseException {
        Topology t = new PresetTopology(2, 4, 4);
        Scheduler s = new Scheduler(t, t.totalThreads());

        SchedulingClass scl = new SchedulingClass(AffinityMode.LOCAL, 2);
        scl.setPackage(0, 0);
        scl.setPackage(1, 1);
        scl.setCore(0, 0);
        scl.setCore(1, 1);

        CPUMap cpuMap = s.tryAcquire(scl);
        Assert.assertNotNull("Should be scheduled", cpuMap);
        int[] schedule = cpuMap.actorMap();

        Assert.assertTrue("Should be scheduled at the different packages",
                t.threadToPackage(schedule[0]) != t.threadToPackage(schedule[1]));
        Assert.assertTrue("Should be scheduled at the different cores",
                t.threadToCore(schedule[0]) != t.threadToCore(schedule[1]));
    }

    @Test
    public void testPermutations_1_1() {
        int[][] ints = Scheduler.classPermutation(1, 1);
        Assert.assertEquals(1, ints.length);
        Assert.assertArrayEquals(new int[] {0}, ints[0]);
    }

    @Test
    public void testPermutations_1_2() {
        int[][] ints = Scheduler.classPermutation(1, 2);
        Assert.assertEquals(1, ints.length);
        Assert.assertArrayEquals(new int[] {0}, ints[0]);
    }

    @Test
    public void testPermutations_2_1() {
        int[][] ints = Scheduler.classPermutation(2, 1);

        Assert.assertEquals(1, ints.length);
        Assert.assertArrayEquals(new int[] {0, 0}, ints[0]);
    }

    @Test
    public void testPermutations_2_2() {
        int[][] ints = Scheduler.classPermutation(2, 2);

        Assert.assertEquals(2, ints.length);
        Assert.assertArrayEquals(new int[] {0, 0}, ints[0]);
        Assert.assertArrayEquals(new int[] {0, 1}, ints[1]);
    }

    @Test
    public void testPermutations_2_3() {
        int[][] ints = Scheduler.classPermutation(2, 3);

        Assert.assertEquals(2, ints.length);
        Assert.assertArrayEquals(new int[] {0, 0}, ints[0]);
        Assert.assertArrayEquals(new int[] {0, 1}, ints[1]);
    }

    @Test
    public void testPermutations_3_1() {
        int[][] ints = Scheduler.classPermutation(3, 1);

        Assert.assertEquals(1, ints.length);
        Assert.assertArrayEquals(new int[] {0, 0, 0}, ints[0]);
    }

    @Test
    public void testPermutations_3_2() {
        int[][] ints = Scheduler.classPermutation(3, 2);

        Assert.assertEquals(4, ints.length);
        Assert.assertArrayEquals(new int[] {0, 0, 0}, ints[0]);
        Assert.assertArrayEquals(new int[] {0, 1, 0}, ints[1]);
        Assert.assertArrayEquals(new int[] {0, 0, 1}, ints[2]);
        Assert.assertArrayEquals(new int[] {0, 1, 1}, ints[3]);
    }

    @Test
    public void testPermutations_3_3() {
        int[][] ints = Scheduler.classPermutation(3, 3);

        Assert.assertEquals(5, ints.length);
        Assert.assertArrayEquals(new int[] {0, 0, 0}, ints[0]);
        Assert.assertArrayEquals(new int[] {0, 1, 0}, ints[1]);
        Assert.assertArrayEquals(new int[] {0, 0, 1}, ints[2]);
        Assert.assertArrayEquals(new int[] {0, 1, 1}, ints[3]);
        Assert.assertArrayEquals(new int[] {0, 1, 2}, ints[4]);
    }

    @Test
    public void testPermutations_3_4() {
        int[][] ints = Scheduler.classPermutation(3, 4);

        Assert.assertEquals(5, ints.length);
        Assert.assertArrayEquals(new int[] {0, 0, 0}, ints[0]);
        Assert.assertArrayEquals(new int[] {0, 1, 0}, ints[1]);
        Assert.assertArrayEquals(new int[] {0, 0, 1}, ints[2]);
        Assert.assertArrayEquals(new int[] {0, 1, 1}, ints[3]);
        Assert.assertArrayEquals(new int[] {0, 1, 2}, ints[4]);
    }

    @Test
    public void testPermutations_4_1() {
        int[][] ints = Scheduler.classPermutation(4, 1);

        Assert.assertEquals(1, ints.length);

        Assert.assertArrayEquals(new int[] {0, 0, 0, 0}, ints[0]);
    }

    @Test
    public void testPermutations_4_2() {
        int[][] ints = Scheduler.classPermutation(4, 2);

        Assert.assertEquals(8, ints.length);

        Assert.assertArrayEquals(new int[] {0, 0, 0, 0}, ints[0]);
        Assert.assertArrayEquals(new int[] {0, 1, 0, 0}, ints[1]);
        Assert.assertArrayEquals(new int[] {0, 0, 1, 0}, ints[2]);
        Assert.assertArrayEquals(new int[] {0, 1, 1, 0}, ints[3]);
        Assert.assertArrayEquals(new int[] {0, 0, 0, 1}, ints[4]);
        Assert.assertArrayEquals(new int[] {0, 1, 0, 1}, ints[5]);
        Assert.assertArrayEquals(new int[] {0, 0, 1, 1}, ints[6]);
        Assert.assertArrayEquals(new int[] {0, 1, 1, 1}, ints[7]);
    }

    @Test
    public void testPermutations_4_3() {
        int[][] ints = Scheduler.classPermutation(4, 3);

        Assert.assertEquals(14, ints.length);

        Assert.assertArrayEquals(new int[] {0, 0, 0, 0}, ints[0]);
        Assert.assertArrayEquals(new int[] {0, 1, 0, 0}, ints[1]);
        Assert.assertArrayEquals(new int[] {0, 0, 1, 0}, ints[2]);
        Assert.assertArrayEquals(new int[] {0, 1, 1, 0}, ints[3]);
        Assert.assertArrayEquals(new int[] {0, 1, 2, 0}, ints[4]);
        Assert.assertArrayEquals(new int[] {0, 0, 0, 1}, ints[5]);
        Assert.assertArrayEquals(new int[] {0, 1, 0, 1}, ints[6]);
        Assert.assertArrayEquals(new int[] {0, 0, 1, 1}, ints[7]);
        Assert.assertArrayEquals(new int[] {0, 1, 1, 1}, ints[8]);
        Assert.assertArrayEquals(new int[] {0, 1, 2, 1}, ints[9]);
        Assert.assertArrayEquals(new int[] {0, 1, 0, 2}, ints[10]);
        Assert.assertArrayEquals(new int[] {0, 0, 1, 2}, ints[11]);
        Assert.assertArrayEquals(new int[] {0, 1, 1, 2}, ints[12]);
        Assert.assertArrayEquals(new int[] {0, 1, 2, 2}, ints[13]);
    }

    @Test
    public void testPermutations_4_4() {
        int[][] ints = Scheduler.classPermutation(4, 4);

        Assert.assertEquals(15, ints.length);

        Assert.assertArrayEquals(new int[] {0, 0, 0, 0}, ints[0]);
        Assert.assertArrayEquals(new int[] {0, 1, 0, 0}, ints[1]);
        Assert.assertArrayEquals(new int[] {0, 0, 1, 0}, ints[2]);
        Assert.assertArrayEquals(new int[] {0, 1, 1, 0}, ints[3]);
        Assert.assertArrayEquals(new int[] {0, 1, 2, 0}, ints[4]);
        Assert.assertArrayEquals(new int[] {0, 0, 0, 1}, ints[5]);
        Assert.assertArrayEquals(new int[] {0, 1, 0, 1}, ints[6]);
        Assert.assertArrayEquals(new int[] {0, 0, 1, 1}, ints[7]);
        Assert.assertArrayEquals(new int[] {0, 1, 1, 1}, ints[8]);
        Assert.assertArrayEquals(new int[] {0, 1, 2, 1}, ints[9]);
        Assert.assertArrayEquals(new int[] {0, 1, 0, 2}, ints[10]);
        Assert.assertArrayEquals(new int[] {0, 0, 1, 2}, ints[11]);
        Assert.assertArrayEquals(new int[] {0, 1, 1, 2}, ints[12]);
        Assert.assertArrayEquals(new int[] {0, 1, 2, 2}, ints[13]);
        Assert.assertArrayEquals(new int[] {0, 1, 2, 3}, ints[14]);
    }

    @Test
    public void testPermutations_4_5() {
        int[][] ints = Scheduler.classPermutation(4, 5);

        Assert.assertEquals(15, ints.length);

        Assert.assertArrayEquals(new int[] {0, 0, 0, 0}, ints[0]);
        Assert.assertArrayEquals(new int[] {0, 1, 0, 0}, ints[1]);
        Assert.assertArrayEquals(new int[] {0, 0, 1, 0}, ints[2]);
        Assert.assertArrayEquals(new int[] {0, 1, 1, 0}, ints[3]);
        Assert.assertArrayEquals(new int[] {0, 1, 2, 0}, ints[4]);
        Assert.assertArrayEquals(new int[] {0, 0, 0, 1}, ints[5]);
        Assert.assertArrayEquals(new int[] {0, 1, 0, 1}, ints[6]);
        Assert.assertArrayEquals(new int[] {0, 0, 1, 1}, ints[7]);
        Assert.assertArrayEquals(new int[] {0, 1, 1, 1}, ints[8]);
        Assert.assertArrayEquals(new int[] {0, 1, 2, 1}, ints[9]);
        Assert.assertArrayEquals(new int[] {0, 1, 0, 2}, ints[10]);
        Assert.assertArrayEquals(new int[] {0, 0, 1, 2}, ints[11]);
        Assert.assertArrayEquals(new int[] {0, 1, 1, 2}, ints[12]);
        Assert.assertArrayEquals(new int[] {0, 1, 2, 2}, ints[13]);
        Assert.assertArrayEquals(new int[] {0, 1, 2, 3}, ints[14]);
    }

}
