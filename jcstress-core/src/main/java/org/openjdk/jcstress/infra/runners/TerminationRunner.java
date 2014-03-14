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
package org.openjdk.jcstress.infra.runners;

import org.openjdk.jcstress.Options;
import org.openjdk.jcstress.infra.collectors.TestResultCollector;
import org.openjdk.jcstress.tests.TerminationTest;
import org.openjdk.jcstress.util.Counter;
import org.openjdk.jcstress.util.HashCounter;
import org.openjdk.jcstress.util.VMSupport;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class TerminationRunner<S> extends Runner<TerminationRunner.OutcomeResult> {
    final TerminationTest<S> test;

    public TerminationRunner(Options opts,  TerminationTest<S> test, TestResultCollector collector, ExecutorService pool) throws FileNotFoundException, JAXBException {
        super(opts, collector, pool, test.getClass().getName());
        this.test = test;
    }

    /**
     * Run the test.
     * This method blocks until test is complete
     */
    public void run() {
        testLog.println("Running " + testName);

        Counter<OutcomeResult> results = new HashCounter<OutcomeResult>();

        testLog.print("Iterations ");
        for (int c = 0; c < control.iters; c++) {
            try {
                VMSupport.tryDeoptimizeAllInfra(control.deoptRatio);
            } catch (NoClassDefFoundError err) {
                // gracefully "handle"
            }

            testLog.print(".");
            testLog.flush();
            run(results);

            dump(testName, results);

            if (results.count(OutcomeResult.of(OutcomeResult.Outcome.STALE)) > 0) {
                testLog.println("Have stale threads, forcing VM to exit");
                testLog.flush();
                testLog.close();
                System.exit(0);
            }
        }
        testLog.println();
    }

    @Override
    public void sanityCheck() throws Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    public Counter<OutcomeResult> internalRun() {
        throw new UnsupportedOperationException();
    }

    private static class Holder<S> {
        volatile S state;
        volatile boolean terminated;
        volatile boolean error;
    }

    public static class OutcomeResult {
        private Outcome outcome;

        public OutcomeResult(Outcome outcome) {
            this.outcome = outcome;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OutcomeResult that = (OutcomeResult) o;

            if (outcome != that.outcome) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return outcome != null ? outcome.hashCode() : 0;
        }

        private enum Outcome {
            TERMINATED,
            STALE,
            ERROR,
        }

        public static OutcomeResult of(Outcome outcome) {
            return new OutcomeResult(outcome);
        }
    }

    private void run(Counter<OutcomeResult> results) {
        long target = System.currentTimeMillis() + control.time;
        while (System.currentTimeMillis() < target) {

            final Holder<S> holder = new Holder<S>();

            holder.state = test.newState();

            Thread t1 = new Thread(new Runnable() {
                public void run() {
                    try {
                        test.actor1(holder.state);
                    } catch (Exception e) {
                        holder.error = true;
                    }
                    holder.terminated = true;
                }
            });
            t1.start();

            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                // do nothing
            }

            try {
                test.signal(holder.state, t1);
            } catch (Exception e) {
                holder.error = true;
            }

            try {
                t1.join(1000);
            } catch (InterruptedException e) {
                // do nothing
            }

            if (holder.terminated) {
                if (holder.error) {
                    results.record(OutcomeResult.of(OutcomeResult.Outcome.ERROR));
                } else {
                    results.record(OutcomeResult.of(OutcomeResult.Outcome.TERMINATED));
                }
            } else {
                results.record(OutcomeResult.of(OutcomeResult.Outcome.STALE));
                return;
            }
        }
    }

}
