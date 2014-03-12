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
public class TerminationRunner<S> extends Runner {
    final TerminationTest<S> test;
    final String testName;

    public TerminationRunner(Options opts,  TerminationTest<S> test, TestResultCollector collector, ExecutorService pool) throws FileNotFoundException, JAXBException {
        super(opts, collector, pool);
        this.test = test;
        this.testName = test.getClass().getName();
    }

    /**
     * Run the test.
     * This method blocks until test is complete
     */
    public void run() {
        testLog.println("Running " + testName);

        HashCounter<Outcome> results = new HashCounter<Outcome>();

        testLog.print("Iterations ");
        for (int c = 0; c < iters; c++) {
            try {
                VMSupport.tryDeoptimizeAllInfra(deoptRatio);
            } catch (NoClassDefFoundError err) {
                // gracefully "handle"
            }

            testLog.print(".");
            testLog.flush();
            run(time, results);

            dump(testName, results);

            if (results.count(Outcome.STALE) > 0) {
                warn("Have stale threads, forcing VM to exit");
                hardExit();
            }
        }
        testLog.println();
    }

    @Override
    public int requiredThreads() {
        return 2;
    }

    private static class Holder<S> {
        volatile S state;
        volatile boolean terminated;
        volatile boolean error;
    }

    private enum Outcome {
        TERMINATED,
        STALE,
        ERROR,
    }

    private void run(int time, Counter<Outcome> results) {
        long target = System.currentTimeMillis() + time;
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
                    results.record(Outcome.ERROR);
                } else {
                    results.record(Outcome.TERMINATED);
                }
            } else {
                results.record(Outcome.STALE);
                return;
            }
        }
    }

}
