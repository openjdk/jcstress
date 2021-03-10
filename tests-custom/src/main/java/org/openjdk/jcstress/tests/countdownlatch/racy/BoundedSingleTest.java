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
package org.openjdk.jcstress.tests.countdownlatch.racy;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@JCStressTest
@Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Received racy CDL, and unlocked fine")
@State
public class BoundedSingleTest {

    CountDownLatch latch;

    static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(BoundedSingleTest.class, "latch", CountDownLatch.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Actor // TODO: There should be the jcstress API for this...
    public void preparer() {
        latch = new CountDownLatch(1);
    }

    @Actor
    public void actor1() {
        CountDownLatch latch = getLatch();
        latch.countDown();
    }

    @Actor
    public void actor2(I_Result r) {
        try {
            CountDownLatch latch = getLatch();
            boolean success = latch.await(1, TimeUnit.DAYS);
            r.r1 = success ? 1 : -1;
        } catch (InterruptedException e) {
            r.r1 = -1;
        }
    }

    private CountDownLatch getLatch() {
        CountDownLatch latch;
        do {
            latch = (CountDownLatch) VH.getOpaque(this);
        } while (latch == null);
        return latch;
    }

}
