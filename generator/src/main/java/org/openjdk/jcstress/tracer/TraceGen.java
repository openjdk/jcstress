/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress.tracer;

import java.util.*;

public class TraceGen {

    private final int vars;

    public TraceGen(int vars) {
        this.vars = vars;
    }

    public void generate() {
        List<Op> possibleOps = new ArrayList<Op>();
        for (int v = 0; v < vars; v++) {
            for (Op.Type t : Op.Type.values()) {
                possibleOps.add(new Op(t, v));
            }
        }

        List<Trace> traces = Collections.singletonList(new Trace());
        for (int l = 0; l < possibleOps.size(); l++) {
            traces = product(traces, possibleOps);
        }

        List<Trace> newTraces = new ArrayList<Trace>();
        for (Trace trace : traces) {
            if (!trace.hasLoads()) continue;
            if (!trace.hasStores()) continue;

            int constId = 0;
            int resId = 0;
            for (int c = 0; c < trace.ops.size(); c++) {
                Op op = trace.ops.get(c);
                switch (op.getType()) {
                    case LOAD:
                        trace.ops.set(c, op.setResId(resId++));
                        break;
                    case STORE:
                        trace.ops.set(c, op.setResId(constId++));
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
            newTraces.add(trace);
        }
        traces = newTraces;

        List<MultiTrace> multiTraces = new ArrayList<MultiTrace>();
        for (Trace trace : traces) {
            for (int l = 0; l < trace.getLength(); l++) {
                MultiTrace mt = split(trace, l);
                multiTraces.add(mt);
            }
        }

        for (MultiTrace mt : multiTraces) {
            List<Trace> linearTraces = mt.linearize();
            Set<Map<Integer, String>> resultSet = new HashSet<Map<Integer, String>>();

            for (Trace linear : linearTraces) {
                Map<Integer, String> results = linear.interpret();
                resultSet.add(results);
            }

            emit(mt, resultSet);
        }
    }

    private void emit(MultiTrace mt, Set<Map<Integer, String>> results) {
        System.out.println("Processing " + mt);
        for (Map<Integer, String> o : results) {
            System.out.println(o);
        }
        System.out.println();
    }

    private MultiTrace split(Trace trace, int sentinel) {
        Trace left = trace.sub(0, sentinel);
        Trace right = trace.sub(sentinel, trace.getLength());
        return new MultiTrace(left, right);
    }

    private List<Trace> product(List<Trace> traces, List<Op> ops) {
        List<Trace> newTraces = new ArrayList<Trace>();
        for (Trace trace : traces) {
            for (Op op : ops) {
                newTraces.add(trace.pushTail(op));
            }
        }
        return newTraces;
    }

    public class Trace {
        private final List<Op> ops;

        public Trace() {
            ops = new ArrayList<Op>();
        }


        public Trace pushHead(Op op) {
            Trace nT = new Trace();
            nT.ops.add(op);
            nT.ops.addAll(ops);
            return nT;
        }

        public Trace pushTail(Op op) {
            Trace nT = new Trace();
            nT.ops.addAll(ops);
            nT.ops.add(op);
            return nT;
        }

        @Override
        public String toString() {
            return "{" + ops + '}';
        }

        public int getLength() {
            return ops.size();
        }

        public Trace sub(int left, int right) {
            Trace nT = new Trace();
            nT.ops.addAll(ops.subList(left, right));
            return nT;
        }

        public SortedMap<Integer, String> interpret() {
            Map<Integer, String> values = new HashMap<Integer, String>();
            for (int v = 0; v < vars; v++) {
                values.put(v, "DEF");
            }

            SortedMap<Integer, String> resValues = new TreeMap<Integer, String>();

            for (Op op : ops) {
                switch (op.getType()) {
                    case LOAD:
                        String v = values.get(op.getVarId());
                        resValues.put(op.getResId(), v);
                        break;
                    case STORE:
                        values.put(op.getVarId(), "C" + op.getResId());
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }

            return resValues;
        }

        public Trace removeFirst() {
            Trace nT = new Trace();
            nT.ops.addAll(ops);
            nT.ops.remove(0);
            return nT;
        }

        public boolean hasLoads() {
            for (Op op : ops) {
                if (op.getType() == Op.Type.LOAD) return true;
            }
            return false;
        }

        public boolean hasStores() {
            for (Op op : ops) {
                if (op.getType() == Op.Type.STORE) return true;
            }
            return false;
        }
    }

    public class MultiTrace {
        private final List<Trace> traces;

        public MultiTrace(Trace... traces) {
            this.traces = Arrays.asList(traces);
        }

        public MultiTrace(List<Trace> copy) {
            this.traces = copy;
        }

        public List<Trace> linearize() {
            if (traces.isEmpty()) {
                return Collections.singletonList(new Trace());
            }

            List<Trace> newTraces = new ArrayList<Trace>();

            for (int t = 0; t < traces.size(); t++) {
                List<Trace> copy = new ArrayList<Trace>();
                copy.addAll(traces);

                Trace cT = copy.get(t);
                if (cT.ops.isEmpty()) {
                    copy.remove(t);
                    for (Trace trace : new MultiTrace(copy).linearize()) {
                        newTraces.add(trace);
                    }
                } else {
                    Op op = cT.ops.get(0);
                    copy.set(t, cT.removeFirst());

                    if (cT.ops.isEmpty()) {
                        copy.remove(t);
                    }

                    for (Trace trace : new MultiTrace(copy).linearize()) {
                        newTraces.add(trace.pushHead(op));
                    }
                }
            }

            return newTraces;
        }

        @Override
        public String toString() {
            return "{" + traces + '}';
        }
    }

}
