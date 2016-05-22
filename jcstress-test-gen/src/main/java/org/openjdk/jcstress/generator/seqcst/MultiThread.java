/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress.generator.seqcst;

import org.openjdk.jcstress.util.HashMultiset;
import org.openjdk.jcstress.util.Multiset;

import java.util.*;
import java.util.stream.Collectors;

public class MultiThread {
    private final Trace original;
    private final List<Trace> threads;

    public MultiThread(Trace original, Collection<Trace> copy) {
        this.original = original;

        this.threads = new ArrayList<>();
        for (Trace t : copy) {
            if (!t.ops().isEmpty())
                threads.add(t);
        }
    }

    /**
     * @return all executions from linearizing the thread operations.
     */
    public List<Trace> linearize() {
        if (threads.isEmpty()) {
            return Collections.singletonList(new Trace());
        }

        List<Trace> newTraces = new ArrayList<>();

        for (int t = 0; t < threads.size(); t++) {
            List<Trace> copy = new ArrayList<>();
            copy.addAll(threads);

            Trace cT = copy.get(t);
            if (cT.ops().isEmpty()) {
                copy.remove(t);
                newTraces.addAll(new MultiThread(original, copy).linearize());
            } else {
                Op op = cT.ops().get(0);
                copy.set(t, cT.removeFirst());

                if (cT.ops().isEmpty()) {
                    copy.remove(t);
                }

                for (Trace trace : new MultiThread(original, copy).linearize()) {
                    newTraces.add(trace.pushHead(op));
                }
            }
        }

        return newTraces;
    }

    @Override
    public String toString() {
        return "{" + threads + '}';
    }

    public String canonicalId() {
        // Renumber the variable IDs.

        List<Trace> lsTrace = new ArrayList<>();
        lsTrace.addAll(threads);
        Collections.sort(lsTrace,
                Comparator.comparing(Trace::loadStoreSeq)
                          .thenComparing(Trace::id));

        int varId = 0;
        Map<Integer, Integer> varMap = new HashMap<>();
        for (Trace trace : lsTrace) {
            for (Op op : trace.ops()) {
                Integer id = varMap.get(op.getVarId());
                if (id == null) {
                    id = varId++;
                    varMap.put(op.getVarId(), id);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Trace trace : lsTrace) {
            for (Op op : trace.ops()) {
                switch (op.getType()) {
                    case LOAD:
                        sb.append("L");
                        break;
                    case STORE:
                        sb.append("S");
                        break;
                    default:
                        throw new IllegalStateException();
                }
                sb.append(varMap.get(op.getVarId()) + 1);
                sb.append("_");
            }
            sb.append("_");
        }
        sb.delete(sb.length() - 2, sb.length());
        return sb.toString();
    }

    public boolean hasNoSingleLoadThreads() {
        for (Trace trace : threads) {
            if (trace.loadCount() == 1 && trace.storeCount() == 0) return false;
        }
        return true;
    }

    public boolean hasNoIntraThreadPairs() {
        Multiset<Integer> vars = new HashMultiset<>();
        for (Trace trace : threads) {
            Set<Integer> touched =
                    trace.ops().stream()
                            .map(Op::getVarId)
                            .collect(Collectors.toSet());
            touched.forEach(vars::add);
        }

        for (Integer v : vars.keys()) {
            if (vars.count(v) == 1) return false;
        }
        return true;
    }

    public boolean isMultiThread() {
        return threads.size() > 1;
    }

    public Set<Result> allResults() {
        return threads.stream()
                .flatMap(t -> t.ops().stream())
                .filter(Op::isLoad)
                .map(Op::getResult)
                .collect(Collectors.toSet());
    }

    public Set<Value> allValues() {
        return threads.stream()
                .flatMap(t -> t.ops().stream())
                .filter(Op::isStore)
                .map(Op::getValue)
                .collect(Collectors.toSet());
    }

    /**
     * @return The set of outcomes where every load can see every store.
     */
    public Set<Map<Result, Value>> racyResults() {
        Set<Map<Result, Value>> allResults = new HashSet<>();
        allResults.add(new HashMap<>());

        for (Result r : allResults()) {
            Set<Map<Result, Value>> temp = new HashSet<>();

            for (Map<Result, Value> sub : allResults) {
                for (Value v : allValues()) {
                    Map<Result, Value> newMap = new HashMap<>(sub);
                    newMap.put(r, v);
                    temp.add(newMap);
                }

                {
                    Map<Result, Value> newMap = new HashMap<>(sub);
                    newMap.put(r, Value.defaultOne());
                    temp.add(newMap);
                }
            }

            allResults = temp;
        }

        return allResults;
    }

    public int loadCount() {
        int r = 0;
        for (Trace trace : threads) {
            r += trace.loadCount();
        }
        return r;
    }

    public List<Trace> threads() {
        return threads;
    }
}
