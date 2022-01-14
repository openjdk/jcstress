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
import org.openjdk.jcstress.util.TreesetMultimap;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractTopology implements Topology {

    private SortedSet<Integer> packages = new TreeSet<>();
    private SortedSet<Integer>   cores    = new TreeSet<>();
    private SortedSet<Integer> threads  = new TreeSet<>();

    private SortedMap<Integer, Integer> threadToPackage = new TreeMap<>();
    private SortedMap<Integer, Integer> threadToCore    = new TreeMap<>();
    private SortedMap<Integer, Integer> coreToPackage   = new TreeMap<>();

    private Multimap<Integer, Integer>  coreToThread    = new TreesetMultimap<>();
    private Multimap<Integer, Integer>  packageToCore   = new TreesetMultimap<>();

    private SortedMap<Integer, Integer> threadToRealCPU = new TreeMap<>();

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
        threadToRealCPU.put(threadId, threadId);

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

    private <K, V> Multimap<K, V> remapKeys(Multimap<K, V> src, Map<K, K> remap) {
        TreesetMultimap<K, V> dst = new TreesetMultimap<>();
        for (K k : src.keys()) {
            K nk = remap.get(k);
            for (V v : src.get(k)) {
                dst.put(nk, v);
            }
        }
        return dst;
    }

    private <K, V> Multimap<K, V> remapValues(Multimap<K, V> src, Map<V, V> remap) {
        TreesetMultimap<K, V> dst = new TreesetMultimap<>();
        for (K k : src.keys()) {
            for (V v : src.get(k)) {
                dst.put(k, remap.get(v));
            }
        }
        return dst;
    }

    private <K, V> SortedMap<K, V> remapValues(SortedMap<K, V> src, Map<V, V> remap) {
        SortedMap<K, V> dst = new TreeMap<>();
        for (K k : src.keySet()) {
            dst.put(k, remap.get(src.get(k)));
        }
        return dst;
    }

    private <K, V> SortedMap<K, V> remapKeys(SortedMap<K, V> src, Map<K, K> remap) {
        SortedMap<K, V> dst = new TreeMap<>();
        for (K k : src.keySet()) {
            dst.put(remap.get(k), src.get(k));
        }
        return dst;
    }

    private <K> Map<K, K> renumber(Set<K> src, Function<Integer, K> gen) {
        Map<K, K> dst = new HashMap<>();
        int nid = 0;
        for (K k : src) {
            dst.put(k, gen.apply(nid++));
        }
        return dst;
    }

    protected void renumberAll() {
        renumberPackages();
        renumberCores();
        renumberThreads();
    }

    private void renumberCores() {
        checkNotFinished();

        Map<Integer, Integer> renumber = renumber(cores, x -> x);

        cores = new TreeSet<>(renumber.values());
        coreToThread  = remapKeys(coreToThread, renumber);
        coreToPackage = remapKeys(coreToPackage, renumber);
        threadToCore  = remapValues(threadToCore, renumber);
        packageToCore = remapValues(packageToCore, renumber);
    }

    private void renumberThreads() {
        checkNotFinished();

        Map<Integer, Integer> renumber = renumber(threads, x -> x);
        for (Integer ot : threads) {
            threadToRealCPU.put(renumber.get(ot), ot);
        }

        threads = new TreeSet<>(renumber.values());
        coreToThread = remapValues(coreToThread, renumber);
        threadToCore = remapKeys(threadToCore, renumber);
        threadToPackage = remapKeys(threadToPackage, renumber);
    }

    private void renumberPackages() {
        checkNotFinished();

        Map<Integer, Integer> renumber = renumber(packages, x -> x);

        packages = new TreeSet<>(renumber.values());
        threadToPackage = remapValues(threadToPackage, renumber);
        coreToPackage = remapValues(coreToPackage, renumber);
        packageToCore = remapKeys(packageToCore, renumber);
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

        for (Integer p : packageToCore.keys()) {
            int size = packageToCore.get(p).size();
            if (coresPerPackage == -1) {
                coresPerPackage = size;
            } else {
                coresPerPackage = Math.min(coresPerPackage, size);
            }
        }

        for (Integer c : coreToThread.keys()) {
            int size = coreToThread.get(c).size();
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
        pw.println("  CPU topology:");
        for (Integer pack : packages) {
            for (Integer core : packageToCore.get(pack)) {
                for (Integer thread : coreToThread.get(core)) {
                    pw.printf("    CPU %s: package #%d, core #%d, thread #%d%n",
                            String.format("%3s", "#" + threadToRealCPU.get(thread)),
                            pack, core, thread);
                }
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

    @Override
    public int threadToRealCPU(int thread) {
        checkFinished();
        Integer v = threadToRealCPU.get(thread);
        if (v == null) {
            throw new IllegalArgumentException("Cannot find real CPU mapping for thread " + thread);
        }
        return v;
    }

}
