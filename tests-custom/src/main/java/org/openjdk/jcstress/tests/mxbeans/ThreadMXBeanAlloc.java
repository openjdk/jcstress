/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jcstress.tests.mxbeans;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.ZZ_Result;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest
@Outcome(id = "true, true",  expect = ACCEPTABLE,             desc = "Delta is >= 0")
@Outcome(                    expect = ACCEPTABLE_INTERESTING, desc = "At least one thread experiences delta <0")
@State
public class ThreadMXBeanAlloc {

    static final com.sun.management.ThreadMXBean BEAN =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    static final AtomicReferenceFieldUpdater<ThreadMXBeanAlloc, Long> UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(ThreadMXBeanAlloc.class, Long.class,"v");

    final long threadId = Thread.currentThread().getId();

    // Deliberately a wrapper to make allocations with boxing
    volatile Long v;

    @Actor
    public void actor1(ZZ_Result r) {
        r.r1 = checkDelta();
    }

    @Actor
    public void actor2(ZZ_Result r) {
        r.r2 = checkDelta();
    }

    boolean checkDelta() {
        long newM = BEAN.getThreadAllocatedBytes(threadId);
        Long prevM = UPDATER.getAndSet(this, newM);
        if (prevM != null) {
            long curM = BEAN.getThreadAllocatedBytes(threadId);
            return curM >= prevM;
        } else {
            return true;
        }
    }
}
