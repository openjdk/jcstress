/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jcstress.generator.ResultGenerator;
import org.openjdk.jcstress.generator.TestGenerator;
import org.openjdk.jcstress.generator.Utils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class TraceGen {

    private final int vars;
    private final String srcDir;
    private final String resDir;
    private final ResultGenerator resultGenerator;

    public TraceGen(int vars, String srcDir, String resDir) {
        this.vars = vars;
        this.srcDir = srcDir;
        this.resDir = resDir;
        this.resultGenerator = new ResultGenerator(srcDir);
    }

    public void generate() {
        List<Op> possibleOps = new ArrayList<>();
        for (int v = 0; v < vars; v++) {
            for (Op.Type t : Op.Type.values()) {
                possibleOps.add(new Op(t, v));
            }
        }

        List<Trace> allTraces = new ArrayList<>();
        List<Trace> traces = Collections.singletonList(new Trace());
        for (int l = 0; l < possibleOps.size(); l++) {
            traces = product(traces, possibleOps);
            allTraces.addAll(traces);
        }
        System.out.println(traces.size() + " basic traces");

        List<Trace> newTraces = new ArrayList<>();
        for (Trace trace : allTraces) {
            if (!trace.hasLoads()) continue;
            if (!trace.hasStores()) continue;
            if (trace.hasNonMatchingLoads()) continue;
            if (trace.hasNonMatchingStores()) continue;
            if (!trace.canonicalId().equals(trace.id())) continue;

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
        System.out.println(traces.size() + " interesting traces");

        List<MultiTrace> multiTraces = new ArrayList<>();
        for (Trace trace : traces) {
            for (int l1 = 0; l1 < trace.getLength(); l1++) {
                for (int l2 = l1; l2 < trace.getLength(); l2++) {
                    for (int l3 = l2; l3 < trace.getLength(); l3++) {
                        MultiTrace mt = split(trace, l1, l2, l3);
                        multiTraces.add(mt);
                    }
                }
            }
        }
        System.out.println(multiTraces.size() + " basic multi-traces");

        Set<String> processedMultitraces = new HashSet<>();

        int testCount = 0;
        for (MultiTrace mt : multiTraces) {
            if (!processedMultitraces.add(mt.canonicalId())) continue;

            List<Trace> linearTraces = mt.linearize();
            Set<Map<Integer, Integer>> scResults = new HashSet<>();
            Set<Map<Integer, Integer>> allResults = new HashSet<>();

            for (Trace linear : linearTraces) {
                SortedMap<Integer, Integer> results = linear.interpret();
                scResults.add(results);
            }

            for (Trace perm : mt.original.allPermutations()) {
                SortedMap<Integer, Integer> results = perm.interpret();
                allResults.add(results);
            }

            // regardless of the reorderings, all results appear SC.
            //    => the violations are undetectable
            assert allResults.containsAll(scResults);
            if (scResults.equals(allResults)) continue;

            List<String> mappedResult = new ArrayList<>();
            for (Map<Integer, Integer> m : scResults) {
                List<String> mappedValues = new ArrayList<>();
                for (int v : m.values()) {
                    mappedValues.add(mapConst(v));
                }
                mappedResult.add(mappedValues.toString());
            }

            emit(mt, mappedResult);
            if ((testCount++ % 1000) == 0)
                System.out.print(".");
        }
        System.out.println();
        System.out.println(testCount + " interesting multi-traces");
    }

    private void emit(MultiTrace mt, List<String> results) {

        final String pkg = "org.openjdk.jcstress.tests.seqcst.volatiles";

        String pathname = Utils.ensureDir(srcDir + "/" + pkg.replaceAll("\\.", "/"));

        String klass = mt.id() + "Test";

        Class[] klasses = new Class[mt.original.getLoadCount()];
        for (int c = 0; c < klasses.length; c++) {
            klasses[c] = int.class;
        }

        String resultName = resultGenerator.generateResult(new TestGenerator.Types(klasses));

        PrintWriter pw;
        try {
            pw = new PrintWriter(pathname + "/" + klass + ".java");
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }

        int threads = mt.traces.size();

        pw.println("package " + pkg + ";");
        pw.println();
        pw.println("import org.openjdk.jcstress.infra.results." + resultName + ";");
        pw.println("import org.openjdk.jcstress.annotations.Actor;");
        pw.println("import org.openjdk.jcstress.annotations.JCStressTest;");
        pw.println("import org.openjdk.jcstress.annotations.State;");
        pw.println("import org.openjdk.jcstress.annotations.Outcome;");
        pw.println("import org.openjdk.jcstress.annotations.Expect;");
        pw.println();
        pw.println("@JCStressTest");
        for (String r : results) {
            pw.println("@Outcome(id = \"" + r + "\", expect = Expect.ACCEPTABLE, desc = \"Sequential consistency.\")");
        }

        pw.println("@State");
        pw.println("public class " + klass + " {");
        pw.println();

        Set<Integer> exist = new HashSet<>();
        for (Trace trace : mt.traces)  {
            for (Op op : trace.ops) {
                if (exist.add(op.getVarId()))
                    pw.println("   volatile int x" + op.getVarId() + ";");
            }
        }
        pw.println();

        for (int t = 0; t < threads; t++) {
            pw.println("    @Actor");
            pw.println("    public void actor" + (t+1) + "(" + resultName + " r) {");

            for (Op op : mt.traces.get(t).ops) {
                switch (op.getType()) {
                    case LOAD:
                        pw.println("        r.r" + (op.getResId() + 1) + " = x" + op.getVarId() + ";");
                        break;
                    case STORE:
                        pw.println("        x" + op.getVarId() + " = " + mapConst(op.getResId()) + ";");
                        break;
                }
            }

            pw.println("    }");
            pw.println();
        }

        pw.println("}");

        pw.close();
    }

    private String mapConst(int resId) {
        return String.valueOf(resId + 1);
    }

    private MultiTrace split(Trace trace, int s1, int s2, int s3) {
        Trace t1 = trace.sub(0, s1);
        Trace t2 = trace.sub(s1, s2);
        Trace t3 = trace.sub(s2, s3);
        Trace t4 = trace.sub(s3, trace.getLength());
        return new MultiTrace(trace, t1, t2, t3, t4);
    }

    private List<Trace> product(List<Trace> traces, List<Op> ops) {
        List<Trace> newTraces = new ArrayList<>();
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
            ops = new ArrayList<>();
        }

        public Trace(List<Op> extOps) {
            ops = new ArrayList<>(extOps);
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

        public SortedMap<Integer, Integer> interpret() {
            Map<Integer, Integer> values = new HashMap<>();
            for (int v = 0; v < vars; v++) {
                values.put(v, -1);
            }

            SortedMap<Integer, Integer> resValues = new TreeMap<>();

            for (Op op : ops) {
                switch (op.getType()) {
                    case LOAD:
                        int v = values.get(op.getVarId());
                        resValues.put(op.getResId(), v);
                        break;
                    case STORE:
                        values.put(op.getVarId(), op.getResId());
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }

            return resValues;
        }

        public List<Trace> allPermutations() {
            List<Trace> traces = new ArrayList<>();
            for (List<Op> perm : Utils.permutate(ops)) {
                traces.add(new Trace(perm));
            }
            return traces;
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

        public int getLoadCount() {
            int count = 0;
            for (Op op : ops) {
                if (op.getType() == Op.Type.LOAD) count++;
            }
            return count;
        }

        public int getStoreCount() {
            int count = 0;
            for (Op op : ops) {
                if (op.getType() == Op.Type.STORE) count++;
            }
            return count;
        }

        public String id() {
            StringBuilder sb = new StringBuilder();
            for (Op op : ops) {
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
                sb.append(op.getVarId() + 1);
                sb.append("_");
            }
            return sb.toString();
        }

        public String canonicalId() {
            int varId = 0;
            Map<Integer, Integer> varMap = new HashMap<>();
            for (Op op : ops) {
                Integer id = varMap.get(op.getVarId());
                if (id == null) {
                    id = varId++;
                    varMap.put(op.getVarId(), id);
                }
            }

            StringBuilder sb = new StringBuilder();
            for (Op op : ops) {
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
            return sb.toString();
        }

        public boolean hasNonMatchingLoads() {
            Set<Integer> stores = new HashSet<>();
            for (Op op : ops) {
                if (op.getType() == Op.Type.STORE) {
                    stores.add(op.getVarId());
                }
            }

            for (Op op : ops) {
                if (op.getType() == Op.Type.LOAD) {
                    if (!stores.contains(op.getVarId()))
                        return true;
                }
            }

            return false;
        }

        public boolean hasNonMatchingStores() {
            Set<Integer> loads = new HashSet<>();
            for (Op op : ops) {
                if (op.getType() == Op.Type.LOAD) {
                    loads.add(op.getVarId());
                }
            }

            for (Op op : ops) {
                if (op.getType() == Op.Type.STORE) {
                    if (!loads.contains(op.getVarId()))
                        return true;
                }
            }

            return false;
        }
    }

    public class MultiTrace {
        private final Trace original;
        private final List<Trace> traces;

        public MultiTrace(Trace original, Trace... traces) {
            this(original, Arrays.asList(traces));
        }

        public MultiTrace(Trace original, List<Trace> copy) {
            this.original = original;

            this.traces = new ArrayList<>();
            for (Trace t : copy) {
                if (!t.ops.isEmpty())
                    traces.add(t);
            }

            Collections.sort(traces, (o1, o2) -> o1.id().compareTo(o2.id()));
        }

        public List<Trace> linearize() {
            if (traces.isEmpty()) {
                return Collections.singletonList(new Trace());
            }

            List<Trace> newTraces = new ArrayList<>();

            for (int t = 0; t < traces.size(); t++) {
                List<Trace> copy = new ArrayList<>();
                copy.addAll(traces);

                Trace cT = copy.get(t);
                if (cT.ops.isEmpty()) {
                    copy.remove(t);
                    for (Trace trace : new MultiTrace(original, copy).linearize()) {
                        newTraces.add(trace);
                    }
                } else {
                    Op op = cT.ops.get(0);
                    copy.set(t, cT.removeFirst());

                    if (cT.ops.isEmpty()) {
                        copy.remove(t);
                    }

                    for (Trace trace : new MultiTrace(original, copy).linearize()) {
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

        public String id() {
            StringBuilder sb = new StringBuilder();
            for (Trace trace : traces) {
                sb.append(trace.id());
                sb.append("_");
            }
            return sb.toString();
        }

        public String canonicalId() {
            StringBuilder sb = new StringBuilder();
            for (Trace trace : traces) {
                if (trace.getLoadCount() == 1 && trace.getStoreCount() == 0) continue;
                sb.append(trace.id());
                sb.append("_");
            }
            return sb.toString();
        }
    }

}
