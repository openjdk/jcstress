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

import org.openjdk.jcstress.util.Multimap;
import org.openjdk.jcstress.util.StringUtils;
import org.openjdk.jcstress.util.TreesetMultimap;

import java.io.PrintStream;
import java.util.*;

public abstract class AbstractTopology implements Topology {

    private SortedSet<Integer> packages = new TreeSet<>();
    private SortedSet<Integer> cores    = new TreeSet<>();
    private SortedSet<Integer> threads  = new TreeSet<>();

    private SortedMap<Integer, Integer> threadToPackage = new TreeMap<>();
    private SortedMap<Integer, Integer> threadToCore    = new TreeMap<>();
    private SortedMap<Integer, Integer> coreToPackage   = new TreeMap<>();

    private Multimap<Integer, Integer>  coreToThread    = new TreesetMultimap<>();
    private Multimap<Integer, Integer>  packageToCore   = new TreesetMultimap<>();

    private int packagesPerSystem = -1;
    private int coresPerPackage = -1;
    private int threadsPerCore = -1;

    private boolean finished;

    protected void add(int packageId, int coreId, int threadId) throws TopologyParseException {
        String triplet = "P" + packageId + ", C" + coreId + ", T" + threadId;

        if (packageId == -1) {
            throw new TopologyParseException("Package is not initialized: " + triplet);
        }

        if (coreId == -1) {
            throw new TopologyParseException("Core is not initialized: " + triplet);
        }

        if (threadId == -1) {
            throw new TopologyParseException("Thread is not initialized: " + triplet);
        }

        packages.add(packageId);
        cores.add(coreId);

        if (!threads.add(threadId)) {
            throw new TopologyParseException("Duplicate thread ID: " + triplet);
        }

        if (coreToPackage.containsKey(coreId)) {
            Integer ex = coreToPackage.get(coreId);
            if (!ex.equals(packageId)) {
                throw new TopologyParseException("Core belongs to different packages: " + triplet + ", " + ex);
            }
        } else {
            coreToPackage.put(coreId, packageId);
        }

        if (threadToPackage.containsKey(threadId)) {
            Integer ex = threadToPackage.get(threadId);
            if (!ex.equals(packageId)) {
                throw new TopologyParseException("Thread belongs to different packages: " + triplet + ", " + ex);
            }
        } else {
            threadToPackage.put(threadId, packageId);
        }

        if (threadToCore.containsKey(threadId)) {
            Integer ex = threadToCore.get(threadId);
            if (!ex.equals(coreId)) {
                throw new TopologyParseException("Thread belongs to different cores: " + triplet + ", " + ex);
            }
        } else {
            threadToCore.put(threadId, coreId);
        }

        packageToCore.put(packageId, coreId);
        coreToThread.put(coreId, threadId);
    }

    protected void renumberCores() {
        checkNotFinished();

        Map<Integer, Integer> renumberCores = new HashMap<>();
        SortedSet<Integer> nCores = new TreeSet<>();
        {
            int ncId = 0;
            for (int ocId : cores) {
                if (!renumberCores.containsKey(ocId)) {
                    renumberCores.put(ocId, ncId++);
                }
                nCores.add(renumberCores.get(ocId));
            }
        }

        Multimap<Integer, Integer> nCoreToThread = new TreesetMultimap<>();
        for (int ocId : cores) {
            int ncId = renumberCores.get(ocId);
            for (int thread : coreToThread.get(ocId)) {
                nCoreToThread.put(ncId, thread);
            }
        }

        SortedMap<Integer, Integer> nThreadToCore = new TreeMap<>();
        for (int thread : threadToCore.keySet()) {
            nThreadToCore.put(thread, renumberCores.get(threadToCore.get(thread)));
        }

        SortedMap<Integer, Integer> nCoreToPackage = new TreeMap<>();
        for (int ocId : coreToPackage.keySet()) {
            nCoreToPackage.put(renumberCores.get(ocId), coreToPackage.get(ocId));
        }

        Multimap<Integer, Integer> nPackageToCore = new TreesetMultimap<>();
        for (int p : packageToCore.keys()) {
            for (int ocId : packageToCore.get(p)) {
                int ncId = renumberCores.get(ocId);
                nPackageToCore.put(p, ncId);
            }
        }

        cores = nCores;
        coreToThread = nCoreToThread;
        coreToPackage = nCoreToPackage;
        threadToCore = nThreadToCore;
        packageToCore = nPackageToCore;
    }

    protected void renumberPackages() {
        checkNotFinished();

        Map<Integer, Integer> renumberPackages = new HashMap<>();
        SortedSet<Integer> nPackages = new TreeSet<>();
        {
            int npId = 0;
            for (int opId : packages) {
                if (!renumberPackages.containsKey(opId)) {
                    renumberPackages.put(opId, npId++);
                }
                nPackages.add(renumberPackages.get(opId));
            }
        }

        Multimap<Integer, Integer> nPackageToCore = new TreesetMultimap<>();
        for (int opId : packages) {
            int npId = renumberPackages.get(opId);
            for (int core : packageToCore.get(opId)) {
                nPackageToCore.put(npId, core);
            }
        }

        SortedMap<Integer, Integer> nThreadToPackage = new TreeMap<>();
        for (int thread : threadToPackage.keySet()) {
            nThreadToPackage.put(thread, renumberPackages.get(threadToPackage.get(thread)));
        }

        SortedMap<Integer, Integer> nCoreToPackage = new TreeMap<>();
        for (int core : coreToPackage.keySet()) {
            nCoreToPackage.put(core, renumberPackages.get(coreToPackage.get(core)));
        }

        packages = nPackages;
        threadToPackage = nThreadToPackage;
        coreToPackage = nCoreToPackage;
        packageToCore = nPackageToCore;
    }

    protected void finish() throws TopologyParseException {
        checkNotFinished();

        if (packages.first() != 0 || packages.last() != packages.size() - 1) {
            throw new TopologyParseException("Package IDs are not consecutive: " + packages);
        }

        if (cores.first() != 0 || cores.last() != cores.size() - 1) {
            throw new TopologyParseException("Core IDs are not consecutive: " + cores);
        }

        if (threads.first() != 0 || threads.last() != threads.size() - 1) {
            throw new TopologyParseException("Thread IDs are not consecutive: " + threads);
        }

        packagesPerSystem = packages.size();

        for (int p : packageToCore.keys()) {
            int size = packageToCore.get(p).size();
            if (coresPerPackage == -1) {
                coresPerPackage = size;
            } else {
                coresPerPackage = Math.min(coresPerPackage, size);
            }
        }

        for (int p : coreToThread.keys()) {
            int size = coreToThread.get(p).size();
            if (threadsPerCore == -1) {
                threadsPerCore = size;
            } else {
                threadsPerCore = Math.min(threadsPerCore, size);
            }
        }

        finished = true;
    }

    private void checkFinished() {
        if (!finished) {
            throw new IllegalStateException("Should be finished first");
        }
    }

    private void checkNotFinished() {
        if (finished) {
            throw new IllegalStateException("Should not be finished yet");
        }
    }

    @Override
    public void printStatus(PrintStream pw) {
        checkFinished();
        pw.printf("  %d package%s, %d core%s per package, %d thread%s per core%n",
                packagesPerSystem, packagesPerSystem > 1 ? "s" : "",
                coresPerPackage, coresPerPackage > 1 ? "s" : "",
                threadsPerCore, threadsPerCore > 1 ? "s" : "");
        pw.println();
        pw.println("  CPU lists:");
        for (int pack : packages) {
            for (int core : packageToCore.get(pack)) {
                String tl = StringUtils.join(coreToThread.get(core), ", ");
                pw.println("    Package #" + pack + ", Core #" + core + ", Threads: " + tl);
            }
        }
    }

    public int threadsPerCore() {
        checkFinished();
        return threadsPerCore;
    }

    public int coresPerPackage() {
        checkFinished();
        return coresPerPackage;
    }

    public int packagesPerSystem() {
        checkFinished();
        return packagesPerSystem;
    }

    public int totalThreads() {
        checkFinished();
        return threads.size();
    }

    @Override
    public int totalCores() {
        checkFinished();
        return cores.size();
    }

    @Override
    public Collection<Integer> coreThreads(int core) {
        checkFinished();
        return coreToThread.get(core);
    }

    @Override
    public Collection<Integer> packageCores(int packageId) {
        checkFinished();
        return packageToCore.get(packageId);
    }

    @Override
    public int coreToPackage(int coreId) {
        checkFinished();
        return coreToPackage.get(coreId);
    }

    @Override
    public int threadToPackage(int thread) {
        checkFinished();
        Integer v = threadToPackage.get(thread);
        if (v == null) {
            throw new IllegalArgumentException("Cannot find package mapping for thread " + thread);
        }
        return v;
    }

    @Override
    public int threadToCore(int thread) {
        checkFinished();
        Integer v = threadToCore.get(thread);
        if (v == null) {
            throw new IllegalArgumentException("Cannot find core mapping for thread " + thread);
        }
        return v;
    }

}
