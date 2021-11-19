/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress.samples.primitives.mutex;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import java.util.concurrent.Semaphore;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest
@Outcome(id = {"1, 2", "2, 1"}, expect = ACCEPTABLE, desc = "Mutex works")
@Outcome(id = "1, 1",           expect = FORBIDDEN,  desc = "Mutex failure")
@State
public class Mutex_06_Semaphore {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Mutex_06_Semaphore
    */

    /*
      ----------------------------------------------------------------------------------------------------------

        Single-permit Semaphore can be used as a crude mutex too. Of course, this primitive
        is much more flexible, it can admit a few threads at once with more permits.

        On x86_64, AArch64, PPC64:
          RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
            1, 1            0    0.00%   Forbidden  Mutex failure
            1, 2  254,394,919   50.23%  Acceptable  Mutex works
            2, 1  252,081,625   49.77%  Acceptable  Mutex works
     */

    private final Semaphore semaphore = new Semaphore(1);
    private int v;

    @Actor
    public void actor1(II_Result r) {
        try {
            semaphore.acquire();
            // critical section
            r.r1 = ++v;
            semaphore.release();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Actor
    public void actor2(II_Result r) {
        try {
            semaphore.acquire();
            // critical section
            r.r2 = ++v;
            semaphore.release();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
