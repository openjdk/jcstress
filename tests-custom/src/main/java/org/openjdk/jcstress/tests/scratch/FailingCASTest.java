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
package org.openjdk.jcstress.tests.scratch;

import org.openjdk.jcstress.infra.annotations.Actor;
import org.openjdk.jcstress.infra.annotations.ConcurrencyStressTest;
import org.openjdk.jcstress.infra.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult2;
import org.openjdk.jcstress.util.UnsafeHolder;

import java.util.concurrent.atomic.AtomicInteger;

@ConcurrencyStressTest
@State
public class FailingCASTest {

    int x;
    final AtomicInteger y = new AtomicInteger();
    int z;

    @Actor
    public void actor1() {
        x = 1;
        if (!y.compareAndSet(42, 43)) {
            z = 1;
        }
    }

    @Actor
    public void actor2(IntResult2 r) {
        r.r1 = z;
        UnsafeHolder.U.fullFence();
        r.r2 = x;
    }

}
