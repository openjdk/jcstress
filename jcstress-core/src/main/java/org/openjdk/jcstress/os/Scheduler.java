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

import org.openjdk.jcstress.os.topology.Topology;

import java.util.*;

public class Scheduler {

    private boolean debug;

    private final BitSet availableCPUs;
    private final int maxUse;
    private final Topology topology;
    private final BitSet availableCores;
    private int currentActorUse;
    private int currentSystemUse;
    private final PackageRecord[] freeMapPackage;

    public Scheduler(Topology t, int max) {
        topology = t;
        maxUse = max;
        availableCPUs = new BitSet(topology.totalThreads());
        availableCPUs.set(0, topology.totalThreads());
        availableCores = new BitSet(topology.totalCores());
        availableCores.set(0, topology.totalCores());
        freeMapPackage = new PackageRecord[topology.packagesPerSystem()];
        for (int p = 0; p < freeMapPackage.length; p++) {
            freeMapPackage[p] = new PackageRecord(-1, -1);
        }
        recomputeFreeMaps();
    }

    public synchronized CPUMap tryAcquire(SchedulingClass scl) {
        if (currentActorUse + scl.numActors() > maxUse) {
            // Over the limit, break out.
            return null;
        }

        checkInvariants("Before acquire");

        CPUMap cpuMap;

        switch (scl.mode()) {
            case NONE:
                cpuMap = scheduleNone(scl);
                break;
            case GLOBAL:
                cpuMap = scheduleGlobal(scl);
                break;
            case LOCAL:
                cpuMap = scheduleLocal(scl);
                break;
            default:
                throw new IllegalStateException("Unhandled mode");
        }

        recomputeFreeMaps();

        checkInvariants("After acquire");

        return cpuMap;
    }

    private CPUMap scheduleLocal(SchedulingClass scl) {
        // Perform initial assignment of package groups to packages.
        // Take the least busy packages first, to guarantee the rest of
        // the scheduling code sees as much cores on those packages as possible.

        int[] packageGroupToPackage = new int[scl.numPackages()];
        Arrays.fill(packageGroupToPackage, -1);

        int pIdx = 0;
        int[] coreGroupToPackage = new int[scl.numCores()];
        for (int a = 0; a < scl.numActors(); a++) {
            int packageGroup = scl.packages[a];
            if (packageGroup == -1) {
                throw new IllegalStateException("Bad actor map");
            }
            int p = packageGroupToPackage[packageGroup];
            if (p == -1) {
                p = freeMapPackage[pIdx++].id;
                packageGroupToPackage[packageGroup] = p;
            }
            coreGroupToPackage[scl.cores[a]] = p;
        }

        // Need to find enough cores and record them as allocated
        int[] coreGroupToCore = new int[scl.numCores()];
        Arrays.fill(coreGroupToCore, -1);

        for (int coreGroup = 0; coreGroup < scl.numCores(); coreGroup++) {
            // Find next core in the required package
            int wantPackage = coreGroupToPackage[coreGroup];

            int idx = 0;
            boolean found = false;
            while (true) {
                int core = availableCores.nextSetBit(idx);
                if (core < 0) break;

                if (topology.coreToPackage(core) == wantPackage) {
                    coreGroupToCore[coreGroup] = core;
                    availableCores.set(core, false);
                    found = true;
                    break;
                } else {
                    idx = core + 1;
                }
            }

            if (!found) {
                // Allocation failed, revert everything set in this round
                for (int c : coreGroupToCore) {
                    if (c != -1) {
                        availableCores.set(c, true);
                    }
                }
                return null;
            }
        }

        // Roll over actors and fill their core assignments
        int[] actorMap = new int[scl.numActors()];
        Arrays.fill(actorMap, -1);

        for (int aIdx = 0; aIdx < scl.numActors(); aIdx++) {
            int core = coreGroupToCore[scl.cores[aIdx]];

            for (int thread : topology.coreThreads(core)) {
                if (availableCPUs.get(thread)) {
                    availableCPUs.set(thread, false);
                    actorMap[aIdx] = thread;
                    currentActorUse++;
                    break;
                }
            }
        }

        // Roll over the sibling threads and allow them too for system uses
        int[] system = new int[topology.totalThreads()];
        int systemCnt = 0;
        for (int core : coreGroupToCore) {
            for (int thread : topology.coreThreads(core)) {
                if (availableCPUs.get(thread)) {
                    availableCPUs.set(thread, false);
                    system[systemCnt++] = thread;
                    currentSystemUse++;
                }
            }
        }

        for (int a : actorMap) {
            if (a == -1) {
                throw new IllegalStateException("Scheduler error: allocation must have succeeded for " + scl +
                        ", was: " + Arrays.toString(actorMap));
            }
        }

        int[] systemMap = Arrays.copyOf(system, systemCnt);

        int[] coreMap = new int[topology.totalThreads()];
        int[] packageMap = new int[topology.totalThreads()];
        for (int thread : actorMap) {
            packageMap[thread] = topology.threadToPackage(thread);
            coreMap[thread] = topology.threadToCore(thread);
        }
        for (int thread : systemMap) {
            packageMap[thread] = topology.threadToPackage(thread);
            coreMap[thread] = topology.threadToCore(thread);
        }

        return new CPUMap(actorMap, systemMap, packageMap, coreMap);
    }

    private CPUMap scheduleGlobal(SchedulingClass scl) {
        // This ignores per-actor assignments completely.
        // It only allocates a separate core per actor, from the pool of all available cores.

        // Need to find enough cores and record them as allocated
        int[] actorToCore = new int[scl.numActors()];
        Arrays.fill(actorToCore, -1);

        for (int a = 0; a < scl.numActors(); a++) {
            int core = availableCores.nextSetBit(0);
            if (core >= 0) {
                actorToCore[a] = core;
                availableCores.set(core, false);
            } else {
                // Allocation failed, revert everything set in this round
                for (int c : actorToCore) {
                    if (c != -1) {
                        availableCores.set(c, true);
                    }
                }
                return null;
            }
        }

        // Take all affected cores as system assignment
        int[] system = new int[topology.totalThreads()];
        int systemCnt = 0;

        for (int core : actorToCore) {
            for (int thread : topology.coreThreads(core)) {
                if (!availableCPUs.get(thread)) {
                    throw new IllegalStateException("Thread should be free");
                }
                availableCPUs.set(thread, false);
                system[systemCnt++] = thread;
                currentSystemUse++;
            }
        }

        int[] actorMap = new int[scl.numActors()];
        Arrays.fill(actorMap, -1);

        int[] systemMap = Arrays.copyOf(system, systemCnt);

        int[] coreMap = new int[topology.totalThreads()];
        int[] packageMap = new int[topology.totalThreads()];
        Arrays.fill(coreMap, -1);
        Arrays.fill(packageMap, -1);
        for (int thread : systemMap) {
            packageMap[thread] = topology.threadToPackage(thread);
            coreMap[thread] = topology.threadToCore(thread);
        }

        return new CPUMap(actorMap, systemMap, packageMap, coreMap);
    }

    private CPUMap scheduleNone(SchedulingClass scl) {
        // TODO: Does this mean "none" is actually fake?
        return scheduleGlobal(scl);
    }

    private void checkInvariants(String when) {
        if (!debug) return;

        for (int c = 0; c < topology.totalCores(); c++) {
            if (availableCores.get(c)) {
                for (int thread : topology.coreThreads(c)) {
                    if (!availableCPUs.get(thread)) {
                        throw new IllegalStateException(when + ": Available core should have all threads free");
                    }
                }
            }
        }

        int use = 0;
        for (int t = 0; t < topology.totalThreads(); t++) {
            if (!availableCPUs.get(t)) {
                use++;
                if (availableCores.get(topology.threadToCore(t))) {
                    throw new IllegalStateException(when + ": Thread taken from the core, core should not be available");
                }
            }
        }

        final int expected = currentActorUse + currentSystemUse;
        if (use != expected) {
            throw new IllegalStateException(when + ": CPU use counts are inconsistent, counter = " + expected + ", actually taken = " + use);
        }

        for (int p = 0; p < topology.packagesPerSystem(); p++) {
            int avail = 0;
            for (int core : topology.packageCores(p)) {
                if (availableCores.get(core)) {
                    avail++;
                }
            }
            for (PackageRecord pr : freeMapPackage) {
                if (pr.id == p && pr.avail != avail) {
                    throw new IllegalStateException(when + ": Package-core availability counts are inconsistent");
                }
            }
        }
    }

    public synchronized void release(CPUMap cpuMap) {
        checkInvariants("Before release");

        for (int c : cpuMap.actorMap()) {
            if (c != -1) {
                availableCPUs.set(c, true);
                availableCores.set(topology.threadToCore(c), true);
                currentActorUse--;
            }
        }
        for (int c : cpuMap.systemMap()) {
            availableCPUs.set(c, true);
            availableCores.set(topology.threadToCore(c), true);
            currentSystemUse--;
        }

        recomputeFreeMaps();

        checkInvariants("After release");
    }

    private void recomputeFreeMaps() {
        for (int p = 0; p < topology.packagesPerSystem(); p++) {
            int avail = 0;
            for (int core : topology.packageCores(p)) {
                if (availableCores.get(core)) {
                    avail++;
                }
            }
            freeMapPackage[p].id = p;
            freeMapPackage[p].avail = avail;
        }

        Arrays.sort(freeMapPackage);
    }

    public int getActorCpus() {
        return currentActorUse;
    }

    public int getSystemCpus() {
        return currentSystemUse;
    }

    private static class PackageRecord implements Comparable<PackageRecord> {
        int id;
        int avail;

        private PackageRecord(int id, int avail) {
            this.id = id;
            this.avail = avail;
        }

        @Override
        public int compareTo(PackageRecord other) {
            int compare = Integer.compare(other.avail, avail);
            if (compare != 0) {
                return compare;
            }
            return Integer.compare(id, other.id);
        }
    }

    void enableDebug() {
        debug = true;
    }

    public List<SchedulingClass> localAffinityFor(int actors, int threadLimit) {
        List<SchedulingClass> packageCases = new ArrayList<>();

        // Assign package groups
        int[][] packagePerms = classPermutation(actors, topology.packagesPerSystem());
        for (int[] pp : packagePerms) {
            SchedulingClass scl = new SchedulingClass(AffinityMode.LOCAL, actors);
            for (int a = 0; a < actors; a++) {
                scl.setPackage(a, pp[a]);
            }
            packageCases.add(scl);
        }

        // Assign core groups
        List<SchedulingClass> coreCases = new ArrayList<>();
        for (SchedulingClass scl : packageCases) {
            int numPackages = scl.numPackages();
            int[] packageActors = scl.packageActors();

            int[][][] packageCoreAssignments = new int[numPackages][][];
            for (int p = 0; p < numPackages; p++) {
                int[][] perms = classPermutation(packageActors[p], topology.coresPerPackage());
                packageCoreAssignments[p] = perms;
            }

            List<SchedulingClass> temp = new ArrayList<>();
            temp.add(scl);

            for (int p = 0; p < numPackages; p++) {
                List<SchedulingClass> newCases = new ArrayList<>();
                for (SchedulingClass tscl : temp) {
                    // Compute last assigned core class for other packages
                    int coreShift = tscl.numCores();
                    if (coreShift < 0) coreShift = 0;

                    for (int[] coreClasses : packageCoreAssignments[p]) {
                        SchedulingClass nscl = new SchedulingClass(tscl);
                        int ccIdx = 0;
                        for (int i = 0; i < actors; i++) {
                            if (scl.getPackage(i) == p) {
                                int coreClass = coreClasses[ccIdx++] + coreShift;
                                nscl.setCore(i, coreClass);
                            }
                        }
                        newCases.add(nscl);
                    }
                }
                temp = newCases;
            }

            coreCases.addAll(temp);
        }

        // Assign thread groups: every thread gets its own group, by construction.
        // If there are not enough threads to satisfy same-core mapping, filter
        // out the actor map, leaving only exclusive-core configs.
        List<SchedulingClass> threadCases = new ArrayList<>();
        for (SchedulingClass scl : coreCases) {
            boolean enoughThreads = true;
            for (int ca : scl.coreActors()) {
                if (ca > topology.threadsPerCore()) {
                    enoughThreads = false;
                    break;
                }
            }

            if (scl.numActors() > threadLimit) {
                enoughThreads = false;
            }

            if (enoughThreads) {
                threadCases.add(scl);
            }
        }

        return threadCases;
    }

    private static int max(int[] perm) {
        int m = -1;
        for (int p2 : perm) {
            m = Math.max(m, p2);
        }
        return m;
    }

    static int[][] classPermutation(int num, int limit) {
        if (num < 1) {
            throw new IllegalArgumentException("Num should be at least 1: classPermutation(" + num + ", " + limit + ")");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit should be at least 1");
        }

        // Base case
        if (num == 1) {
            return new int[][] { new int[] {0} };
        }

        int[][] oldPerms = classPermutation(num - 1, limit);

        // Compute the max class in old permutations, and the number
        // of permutations with maxed-out classes.
        int oldMax = Math.min(num - 1, limit) - 1;
        int newMax = oldMax + 1;

        int[][] newPerms = new int[oldPerms.length*(newMax+1)][];
        int nIdx = 0;

        // Copy all existing permutations with up to max variability
        for (int nm = 0; nm < newMax; nm++) {
            for (int[] p : oldPerms) {
                int m = max(p);
                if (m >= nm - 1) {
                    int[] np = Arrays.copyOf(p, p.length + 1);
                    np[p.length] = nm;
                    newPerms[nIdx++] = np;
                }
            }
        }

        // Copy all existing permutations with maxed out class
        for (int[] p : oldPerms) {
            int m = max(p);
            if (m == oldMax && m < limit - 1) {
                int[] np = Arrays.copyOf(p, p.length + 1);
                np[p.length] = newMax;
                newPerms[nIdx++] = np;
            }
        }

        return Arrays.copyOf(newPerms, nIdx);
    }

    List<SchedulingClass> globalAffinityFor(int threads, int threadLimit) {
        return noneOrGlobalAffinityFor(AffinityMode.GLOBAL, threads, threadLimit);
    }

    List<SchedulingClass> noneAffinityFor(int threads, int threadLimit) {
        return noneOrGlobalAffinityFor(AffinityMode.NONE, threads, threadLimit);
    }

    private List<SchedulingClass> noneOrGlobalAffinityFor(AffinityMode mode, int threads, int threadLimit) {
        if (threads > topology.totalCores()) {
            return Collections.emptyList();
        }
        if (threads > threadLimit) {
            return Collections.emptyList();
        }
        SchedulingClass scl = new SchedulingClass(mode, threads);
        for (int t = 0; t < threads; t++) {
            scl.setPackage(t, -1);
            scl.setCore(t, -1);
        }
        return Collections.singletonList(scl);
    }

    @SuppressWarnings("fallthrough")
    public List<SchedulingClass> scheduleClasses(int actorThreads, int threadLimit, AffinityMode mode) {
        switch (mode) {
            case LOCAL:
                if (OSSupport.affinitySupportAvailable()) {
                    return localAffinityFor(actorThreads, threadLimit);
                }
            case GLOBAL:
                if (OSSupport.taskSetAvailable()) {
                    return globalAffinityFor(actorThreads, threadLimit);
                }
            case NONE:
                return noneAffinityFor(actorThreads, threadLimit);
            default:
                throw new IllegalStateException("Unhandled affinity mode: " + mode);
        }
    }
}
