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
package org.openjdk.jcstress.samples.concurrency.classic;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.Z_Result;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/*
    How to run this test:
        $ java -jar jcstress-samples/target/jcstress.jar -t ClassicProblem_01_DiningPhilosophers
 */

/**
 * This sample shows you how JCStress can help you to test solutions for the famous Dining philosophers problem.
 * It solves this problem by a resource hierarchy.
 * See https://en.wikipedia.org/wiki/Dining_philosophers_problem for more information about the problem.
 */
public class ClassicProblem_01_DiningPhilosophers {
    @JCStressTest
    @Outcome(id = {"true"}, expect = ACCEPTABLE, desc = "All philosophers could eat with their 2 neighboured forks.")
    @Outcome(expect = FORBIDDEN, desc = "At least one philosopher couldn't eat.")
    @State
    public static class ResourceHierarchy {
        private final Semaphore[] semaphores =
                new Semaphore[]{new Semaphore(1), new Semaphore(1), new Semaphore(1)};

        @Actor
        public void p1() {
            // think
            eat(0, 1);
        }

        @Actor
        public void p2() {
            // think
            eat(1, 2);
        }

        @Actor
        public void p3() {
            // think
            eat(0, 2); // and not eat(2, 0) because we must acquire all locks in the same order to avoid deadlocks
        }

        @Arbiter
        public void fake(Z_Result r) {
            r.r1 = true;
        }

        final protected void eat(int fork1, int fork2) {
            try {
                semaphores[fork1].acquire();
                semaphores[fork2].acquire();
                // eating
                semaphores[fork1].release();
                semaphores[fork2].release();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
