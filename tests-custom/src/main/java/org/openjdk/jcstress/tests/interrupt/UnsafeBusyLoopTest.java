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
package org.openjdk.jcstress.tests.interrupt;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Mode;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.Signal;
import org.openjdk.jcstress.annotations.State;

import static org.openjdk.jcstress.util.UnsafeHolder.UNSAFE;

@JCStressTest(Mode.Termination)
@Outcome(id = "TERMINATED", expect = Expect.ACCEPTABLE, desc = "The thread had sucessfully terminated.")
@Outcome(id = "STALE",      expect = Expect.ACCEPTABLE_INTERESTING, desc = "Thread had failed to respond.")
@State
public class UnsafeBusyLoopTest {

    public static final long offset;
    public boolean isStopped;

    static {
        try {
            offset = UNSAFE.objectFieldOffset(UnsafeBusyLoopTest.class.getDeclaredField("isStopped"));
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    @Actor
    public void actor1() {
        while (!UNSAFE.getBoolean(this, offset)) {
            // burn!
        }
    }

    @Signal
    public void signal() {
        isStopped = true;
    }

}
