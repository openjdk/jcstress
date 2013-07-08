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

import org.openjdk.jcstress.generator.ResultGenerator;
import org.openjdk.jcstress.generator.TestGenerator;
import org.openjdk.jcstress.generator.Utils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class TraceGen {

    private final int vars;
    private final String srcDir;
    private final String resDir;
    private final ResultGenerator resultGenerator;
    private PrintWriter resourceWriter;

    public TraceGen(int vars, String srcDir, String resDir) {
        this.vars = vars;
        this.srcDir = srcDir;
        this.resDir = resDir;
        this.resultGenerator = new ResultGenerator(srcDir);
    }

    public void generate() {
        try {
            resourceWriter = new PrintWriter(Utils.ensureDir(resDir + "/org/openjdk/jcstress/desc/") + "/seqcst-volatiles.xml");
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }
        resourceWriter.println("<testsuite>");

        List<Op> possibleOps = new ArrayList<Op>();
        for (int v = 0; v < vars; v++) {
            for (Op.Type t : Op.Type.values()) {
                possibleOps.add(new Op(t, v));
            }
        }

        List<Trace> allTraces = new ArrayList<Trace>();
        List<Trace> traces = Collections.singletonList(new Trace());
        for (int l = 0; l < possibleOps.size(); l++) {
            traces = product(traces, possibleOps);
            allTraces.addAll(traces);
        }

        List<Trace> newTraces = new ArrayList<Trace>();
        for (Trace trace : allTraces) {
            if (!trace.hasLoads()) continue;
            if (!trace.hasStores()) continue;
            if (trace.hasNonMatchingLoads()) continue;
            if (trace.hasNonMatchingStores()) continue;

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
            Set<Map<Integer, Integer>> resultSet = new HashSet<Map<Integer, Integer>>();

            for (Trace linear : linearTraces) {
                Map<Integer, Integer> results = linear.interpret();
                resultSet.add(results);
            }

            List<String> scResults = new ArrayList<String>();
            for (Map<Integer, Integer> m : resultSet) {
                List<String> mappedValues = new ArrayList<String>();
                for (int v : m.values()) {
                    mappedValues.add(mapConst(v));
                }
                scResults.add(mappedValues.toString());
            }

            if (mt.traces.size() == 2) {
                emit(mt, scResults);
                System.out.print(".");
            }
        }
        System.out.println();

        resourceWriter.println("</testsuite>");
        resourceWriter.close();
    }

    private void emit(MultiTrace mt, List<String> results) {

        final String pkg = "org.openjdk.jcstress.tests.seqconst.volatiles";

        String pathname = Utils.ensureDir(srcDir + "/" + pkg.replaceAll("\\.", "/"));

        String klass = mt.id() + "Test";

        resourceWriter.println("    <test name=\"" + pkg + "." + klass + "\">\n" +
                "        <contributed-by>Aleksey Shipilev (aleksey.shipilev@oracle.com)</contributed-by>\n" +
                "        <description>Generated test</description>\n");

        for (String r : results) {
                resourceWriter.println(
                "        <case>\n" +
                "            <match>" + r + "</match>\n" +
                "            <expect>ACCEPTABLE</expect>\n" +
                "            <description>Autogenerated match</description>\n" +
                "        </case>\n"
            );
        }

        resourceWriter.println(
                "        <unmatched>\n" +
                "            <expect>FORBIDDEN</expect>\n" +
                "            <description>Other cases are not expected.</description>\n" +
                "        </unmatched>\n" +
                "    </test>");


        Class[] klasses = new Class[mt.original.getLoadCount()];
        for (int c = 0; c < klasses.length; c++) {
            klasses[c] = int.class;
        }

        String resultName = resultGenerator.generateResult(new TestGenerator.Types(klasses));

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(pathname + "/" + klass + ".java");
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }

        pw.println("package " + pkg + ";\n" +
                "\n" +
                "import java.util.concurrent.*;\n" +
                "import java.util.concurrent.atomic.*;\n" +
                "import org.openjdk.jcstress.infra.results." + resultName + ";\n" +
                "import org.openjdk.jcstress.tests.Actor2_Test;\n" +
                "\n" +
                "public class " + klass + " implements Actor2_Test<" + klass + ".State, " + resultName + "> {\n" +
                "\n" +
                "    @Override\n" +
                "    public State newState() {\n" +
                "        return new State();\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void actor1(State s, " + resultName + " r) {");

        for (Op op : mt.traces.get(0).ops) {
            switch (op.getType()) {
                case LOAD:
                    pw.println("        r.r" + (op.getResId() + 1) + " = s.x" + op.getVarId() + ";");
                    break;
                case STORE:
                    pw.println("        s.x" + op.getVarId() + " = " + mapConst(op.getResId()) + ";");
                    break;
            }
        }

        pw.println(
                "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public void actor2(State s, " + resultName + " r) {");

        for (Op op : mt.traces.get(1).ops) {
            switch (op.getType()) {
                case LOAD:
                    pw.println("        r.r" + (op.getResId() + 1) + " = s.x" + op.getVarId() + ";");
                    break;
                case STORE:
                    pw.println("        s.x" + op.getVarId() + " = " + mapConst(op.getResId()) + ";");
                    break;
            }
        }

        pw.println(
                "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public " + resultName + " newResult() {\n" +
                        "        return new " + resultName + "();\n" +
                        "    }\n" +
                        "\n" +
                        "    public static class State {");

        Set<Integer> exist = new HashSet<Integer>();
        for (Op op : mt.traces.get(0).ops) {
            if (exist.add(op.getVarId()))
                pw.println("        public volatile int x" + op.getVarId() + ";");
        }
        for (Op op : mt.traces.get(1).ops) {
            if (exist.add(op.getVarId()))
                pw.println("        public volatile int x" + op.getVarId() + ";");
        }
        pw.println("    }");
        pw.println("}");

        pw.close();
    }

    private String mapConst(int resId) {
        return String.valueOf(resId + 1);
    }

    private MultiTrace split(Trace trace, int sentinel) {
        Trace left = trace.sub(0, sentinel);
        Trace right = trace.sub(sentinel, trace.getLength());
        return new MultiTrace(trace, left, right);
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

        public SortedMap<Integer, Integer> interpret() {
            Map<Integer, Integer> values = new HashMap<Integer, Integer>();
            for (int v = 0; v < vars; v++) {
                values.put(v, -1);
            }

            SortedMap<Integer, Integer> resValues = new TreeMap<Integer, Integer>();

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

        public boolean hasNonMatchingLoads() {
            Set<Integer> stores = new HashSet<Integer>();
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
            Set<Integer> loads = new HashSet<Integer>();
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

            this.traces = new ArrayList<Trace>();
            for (Trace t : copy) {
                if (!t.ops.isEmpty())
                    traces.add(t);
            }
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
    }

}
