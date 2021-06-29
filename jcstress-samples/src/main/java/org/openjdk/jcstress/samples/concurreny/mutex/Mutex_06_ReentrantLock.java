/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress.samples.concurreny.mutex;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.ZZ_Result;

import java.util.concurrent.locks.ReentrantLock;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/*
    How to run this test:
        $ java -jar jcstress-samples/target/jcstress.jar -t Mutex_06_ReentrantLock
*/

/**
 * This sample demonstrates you how you can introduce a critical section to check algorithms
 * which ensure only one actor at most can enter the critical section.
 */
@JCStressTest
@Outcome(id = {"false, false"}, expect = ACCEPTABLE, desc = "Both actors have entered the critical section one after another")
@Outcome(expect = FORBIDDEN, desc = "Both actors have entered the critical section at the same time")
@State
public class Mutex_06_ReentrantLock {
    private final ReentrantLock reentrantLock = new ReentrantLock();
    private boolean taken1, taken2;

    @Actor
    public void actor1(ZZ_Result r) {
        reentrantLock.lock();
        try {
            taken1 = true;
            r.r1 = taken2;
            taken1 = false;
        } finally {
            reentrantLock.unlock();
        }
    }

    @Actor
    public void actor2(ZZ_Result r) {
        reentrantLock.lock();
        try {
            taken2 = true;
            r.r2 = taken1;
            taken2 = false;
        } finally {
            reentrantLock.unlock();
        }
    }
}
