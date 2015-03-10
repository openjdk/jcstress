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
package org.openjdk.jcstress.tests.vjug;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult1;
import org.openjdk.jcstress.util.UnsafeHolder;

@JCStressTest
@Description("Tests if Unsafe breaks the atomicity while doing cross cache-line reads/writes.")
@Outcome(id = "[0]",  expect = Expect.ACCEPTABLE,             desc = "Seeing the default value, this is a legal race.")
@Outcome(id = "[-1]", expect = Expect.ACCEPTABLE,             desc = "Seeing the full value, this is a legal behavior.")
@Outcome(             expect = Expect.ACCEPTABLE_INTERESTING, desc = "Other cases exhibit the breach of read/write atomicity.")
@State
public class UnsafeAtomicityTest {

    public final long addr;
    public final long offset;

    public UnsafeAtomicityTest() {
        final int size = 1024;
        addr = UnsafeHolder.U.allocateMemory(size);
        UnsafeHolder.U.setMemory(addr, size, (byte)0);
        offset = ((addr + 512) & ~255) - 2;
    }

    @Actor
    public void actor1() {
        UnsafeHolder.U.putInt(null, offset, 0xFFFFFFFF);
    }

    @Actor
    public void actor2(IntResult1 r) {
        r.r1 = UnsafeHolder.U.getInt(null, offset);
    }

}
