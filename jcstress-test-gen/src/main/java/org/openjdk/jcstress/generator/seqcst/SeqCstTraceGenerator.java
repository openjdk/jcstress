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
package org.openjdk.jcstress.generator.seqcst;

import org.openjdk.jcstress.generator.ResultGenerator;
import org.openjdk.jcstress.generator.TestGenerator;
import org.openjdk.jcstress.generator.Utils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SeqCstTraceGenerator {

    private final String srcDir;
    private final String pkg;
    private final Target target;
    private final ResultGenerator resultGenerator;

    public static void generate(String dst, String pkg, Target target) {
        new SeqCstTraceGenerator(dst, pkg, target).generate();
    }

    private SeqCstTraceGenerator(String dst, String pkg, Target target) {
        this.srcDir = dst;
        this.pkg = pkg;
        this.target = target;
        this.resultGenerator = new ResultGenerator(dst);
    }

    public void generate() {
        /*
           Step 1. Generate all possible traces from the available ops:
         */

        System.out.print("Generating load/store permutations... ");

        List<Trace> allTraces = new ArrayList<>();
        {
            List<Op> possibleOps = new ArrayList<>();
            for (int v = 0; v < 2; v++) {
                possibleOps.add(Op.newStore(v));
                possibleOps.add(Op.newLoad(v));
                possibleOps.add(Op.newLoad(v));
            }

            List<Trace> traces = Collections.singletonList(new Trace());
            for (int l = 0; l < possibleOps.size(); l++) {
                traces = product(traces, possibleOps);
                allTraces.addAll(traces);
            }
        }

        {
            List<Op> possibleOps = new ArrayList<>();
            for (int v = 0; v < 3; v++) {
                possibleOps.add(Op.newStore(v));
                possibleOps.add(Op.newLoad(v));
            }

            List<Trace> traces = Collections.singletonList(new Trace());
            for (int l = 0; l < possibleOps.size(); l++) {
                traces = product(traces, possibleOps);
                allTraces.addAll(traces);
            }
        }

        List<Trace> traces = allTraces;
        System.out.print(" " + traces.size() + " found... ");

        /*
           Step 2. Filter out non-interesting traces.
         */

        Set<String> canonicalTraces = new HashSet<>();
        traces = allTraces.stream()
                .filter(Trace::hasLoads)            // Has observable effects
                .filter(Trace::hasStores)           // Has modifications to observe
                .filter(Trace::matchedLoadStores)   // All modifications are observed; no observing non-modified
                .filter(t -> canonicalTraces.add(t.canonicalId())) // Only a canonical order of vars accepted
                .collect(Collectors.toList());

        for (Trace trace : traces) {
            trace.assignResults();
        }

        System.out.println(traces.size() + " are interesting.");

        /*
           Step 3. Distribute load-stores between threads, yielding all possible scenarios.
         */
        final int THREADS = 4;

        System.out.print("Generating test cases that distribute load/stores among " + THREADS + " threads... ");

        List<MultiThread> multiThreads = new ArrayList<>();
        for (Trace trace : traces) {

            int len = trace.getLength();
            int bitsNeeded = 2*len;
            if (bitsNeeded > 63) {
                throw new IllegalStateException("Cannot handle large traces like that");
            }

            long bound = 1 << bitsNeeded;

            for (long c = 0; c < bound; c++) {
                Map<Integer, List<Op>> newT = new HashMap<>();
                for (int t = 0; t < THREADS; t++) {
                    newT.put(t, new ArrayList<>());
                }

                long ticket = c;
                for (Op op : trace.ops()) {
                    int thread = (int)(ticket & 3);
                    newT.get(thread).add(op);
                    ticket = ticket >> 2;
                }

                MultiThread mt = new MultiThread(trace, newT.values().stream().map(Trace::new).collect(Collectors.toList()));
                multiThreads.add(mt);
            }
        }
        System.out.print(multiThreads.size() + " testcases generated... ");

        /*
           Step 4. Apply more filters to reduce
         */
        Set<String> canonicalIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

        multiThreads = multiThreads.parallelStream()
                .filter(MultiThread::isMultiThread)               // really have multiple threads
                .filter(MultiThread::hasNoSingleLoadThreads)      // threads with single loads produce duplicate tests
                .filter(MultiThread::hasNoIntraThreadPairs)       // has no operations that do not span threads
                .filter(mt -> canonicalIds.add(mt.canonicalId())) // pass only one canonical
                .collect(Collectors.toList());

        System.out.println(multiThreads.size() + " interesting.");

        /*
            Step 5. Figure out what executions are sequentially consistent (needed for grading!),
            and emit the tests.
         */

        Set<String> generatedIds = new HashSet<>();

        System.out.print("Figuring out SC outcomes for the testcases: ");
        int testCount = 0;
        for (MultiThread mt : multiThreads) {
            Set<Map<Result, Value>> scResults = new HashSet<>();

            // Compute all SC results from the linearization of MT
            for (Trace linear : mt.linearize()) {
                SortedMap<Result, Value> results = linear.interpret();
                scResults.add(results);
            }

            Set<Map<Result, Value>> allResults = mt.racyResults();

            if (!allResults.containsAll(scResults)) {
                System.out.println(mt.canonicalId());
                System.out.println(scResults);
                System.out.println(allResults);
                throw new IllegalStateException("SC results should be subset of all results");
            }

            if (scResults.equals(allResults)) {
                // all racy results are indistinguishable from SC
                //    => the violations are undetectable
                // nothing to do here,
                continue;
            }

            generatedIds.add(mt.canonicalId());

            List<String> mappedResult = new ArrayList<>();
            for (Map<Result, Value> m : scResults) {
                List<String> mappedValues = new ArrayList<>();
                for (Value v : m.values()) {
                    mappedValues.add(v.toString());
                }
                mappedResult.add(mappedValues.toString());
            }
            emit(mt, mappedResult);
            if ((testCount++ % 100) == 0)
                System.out.print(".");
        }
        System.out.println();
        System.out.println("Found " + testCount + " interesting test cases");

        /*
            Step 6. Check that no important cases were filtered.

            The nomenclature is derived from Maranget, Sarkar, Sewell,
              "A Tutorial Introduction to the ARM and POWER Relaxed Memory Models"
         */
        check(generatedIds, "L1_L2__S2_S1", "MP");

        // TODO: Have mismatched stores: need arbiter to judge the final result
        // check(generatedIds, "L1_S2__S2_S1", "S");
        // check(generatedIds, "S1_L2__S2_S1", "R");
        // check(generatedIds, "S1_S2__S2_S1", "2+2W");
        // check(generatedIds, "L1_S2__S1__S2_L1", "WRW+WR");
        // check(generatedIds, "L1_S2__S1__S2_S1", "WRR+2W");
        // check(generatedIds, "L1_S2__S2_L3__S3_S1", "Z6.0");
        // check(generatedIds, "L1_S2__S2_S3__S3_S1", "Z6.1");
        // check(generatedIds, "L1_S2__L2_S3__S3_S1", "Z6.2");
        // check(generatedIds, "L1_L2__S2_S3__S3_S1", "Z6.3");
        // check(generatedIds, "S1_L2__S2_L3__S3_S1", "Z6.4");
        // check(generatedIds, "S1_L2__S2_S3__S3_S1", "Z6.5");
        // check(generatedIds, "S1_S2__S2_S3__S3_S1", "3.2W");

        check(generatedIds, "S1_L2__S2_L1", "SB");
        check(generatedIds, "L1_S2__L2_S1", "LB");

        check(generatedIds, "L1_L2__L2_S1__S2", "WRC");
        check(generatedIds, "L1_S2__L2_S1__S1", "WWC");
        check(generatedIds, "L1_L2__S1__S2_L1", "RWC");
        check(generatedIds, "L1_L2__S1__S2_S1", "WRR+2W");

        check(generatedIds, "L1_L2__S2_S1", "PPO");
        check(generatedIds, "L1_L2__L2_L1__S1__S2", "IRIW");
        check(generatedIds, "L1_L2__L2_S1__S1__S2", "IRRWIW");
        check(generatedIds, "L1_S2__L2_S1__S1__S2", "IRWIW");

        check(generatedIds, "L1_L1__S1_S1", "CoRR0");
        check(generatedIds, "L1_L1__S1", "CoRR1");
        check(generatedIds, "L1_L1__L1_L1__S1__S1", "CoRR2");
        check(generatedIds, "L1_S1__S1", "CoRW");
        check(generatedIds, "S1__S1_L1", "CoWR");

        check(generatedIds, "L1_L2__L3_S1__S2_S3", "ISA2");
        check(generatedIds, "L1_S2__L2_S3__L3_S1", "3.LB");
        check(generatedIds, "S1_L2__S2_L3__S3_L1", "3.SB");

        check(generatedIds, "L1_L2__S2_L3__S3_S1", "W+RWC");
    }

    private void check(Set<String> ids, String id, String info) {
        if (!ids.contains(id)) {
            throw new IllegalStateException("Generated cases should contain " + info);
        }
    }

    private void emit(MultiThread mt, List<String> scResults) {
        String pathname = Utils.ensureDir(srcDir + "/" + pkg.replaceAll("\\.", "/"));

        String klass = mt.canonicalId() + "_Test";

        Class[] klasses = new Class[mt.loadCount()];
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

        int threads = mt.threads().size();

        pw.println("package " + pkg + ";");
        pw.println();
        pw.println("import org.openjdk.jcstress.infra.results.*;");
        pw.println("import org.openjdk.jcstress.annotations.*;");
        pw.println();
        pw.println("@JCStressTest");
        pw.println("@Outcome(id = {");
        for (String r : scResults) {
            pw.println("            \"" + r + "\",");
        }
        pw.println("}, expect = Expect.ACCEPTABLE, desc = \"Sequential consistency.\")");

        pw.println("@State");
        pw.println("public class " + klass + " {");
        pw.println();

        Set<Integer> exist = new HashSet<>();
        for (Trace trace : mt.threads())  {
            for (Op op : trace.ops()) {
                if (exist.add(op.getVarId())) {
                    switch (target) {
                        case VOLATILE:
                            pw.println("    volatile int x" + op.getVarId() + ";");
                            break;
                        case SYNCHRONIZED:
                            pw.println("    int x" + op.getVarId() + ";");
                            break;
                        default:
                            throw new IllegalStateException("" + target);
                    }
                }
            }
        }
        pw.println();

        for (int t = 0; t < threads; t++) {
            pw.println("    @Actor");
            pw.println("    public void actor" + (t+1) + "(" + resultName + " r) {");

            for (Op op : mt.threads().get(t).ops()) {
                switch (op.getType()) {
                    case LOAD:
                        if (target == Target.SYNCHRONIZED) {
                            pw.println("        synchronized (this) {");
                            pw.print("    ");
                        }
                        pw.println("        r.r" + op.getResult() + " = x" + op.getVarId() + ";");
                        if (target == Target.SYNCHRONIZED) {
                            pw.println("        }");
                        }
                        break;
                    case STORE:
                        if (target == Target.SYNCHRONIZED) {
                            pw.println("        synchronized (this) {");
                            pw.print("    ");
                        }
                        pw.println("        x" + op.getVarId() + " = " + op.getValue() + ";");
                        if (target == Target.SYNCHRONIZED) {
                            pw.println("        }");
                        }
                        break;
                }
            }

            pw.println("    }");
            pw.println();
        }

        pw.println("}");

        pw.close();
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

}
