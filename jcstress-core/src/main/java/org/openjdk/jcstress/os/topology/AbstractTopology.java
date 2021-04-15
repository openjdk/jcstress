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

    private final SortedSet<Integer> packages = new TreeSet<>();
    private final SortedSet<Integer> threads = new TreeSet<>();
    private final Map<Integer, Integer> threadToPackage = new TreeMap<>();

    private SortedSet<Integer> cores = new TreeSet<>();
    private Map<Integer, Integer> threadToCore = new TreeMap<>();
    private Map<Integer, Integer> coreToPackage = new TreeMap<>();
    private Multimap<Integer, Integer> coreToThread = new TreesetMultimap<>();
    private Multimap<Integer, Integer> packageToCore = new TreesetMultimap<>();

    private int packagesPerSystem = -1;
    private int coresPerPackage = -1;
    private int threadsPerCore = -1;

    private boolean finished;

    protected void add(int packageId, int coreId, int threadId) throws TopologyParseException {
        if (packageId == -1) {
            throw new TopologyParseException("Package is not initialized: " + packageId);
        }

        if (coreId == -1) {
            throw new TopologyParseException("Core is not initialized: " + coreId);
        }

        if (threadId == -1) {
            throw new TopologyParseException("Thread is not initialized: " + threadId);
        }

        packages.add(packageId);
        cores.add(coreId);
        if (!threads.add(threadId)) {
            throw new TopologyParseException("Duplicate thread ID: " + threadId);
        }

        packageToCore.put(packageId, coreId);
        coreToThread.put(coreId, threadId);
        coreToPackage.put(coreId, packageId);
        threadToPackage.put(threadId, packageId);
        threadToCore.put(threadId, coreId);
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

        Map<Integer, Integer> nThreadToCore = new HashMap<>();
        for (int thread : threadToCore.keySet()) {
            nThreadToCore.put(thread, renumberCores.get(threadToCore.get(thread)));
        }

        Map<Integer, Integer> nCoreToPackage = new HashMap<>();
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
