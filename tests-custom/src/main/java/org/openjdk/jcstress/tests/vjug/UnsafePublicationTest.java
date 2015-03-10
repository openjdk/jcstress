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
package org.openjdk.jcstress.tests.vjug;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult1;

@JCStressTest
@Description("Tests if unsafe publication is unsafe.")
@Outcome(id = "[-1]", expect = Expect.ACCEPTABLE,             desc = "The object is not yet published")
@Outcome(id = "[0]",  expect = Expect.ACCEPTABLE_INTERESTING, desc = "The object is published, but the fields has the default value.")
@Outcome(id = "[1]",  expect = Expect.ACCEPTABLE,             desc = "The object is published, and all fields are visible.")
@State
public class UnsafePublicationTest {

    final MyObject o = new MyObject();

    MyObject po;

    @Actor
    public void publish() {
        o.x = 1;
        po = o;
    }

    @Actor
    public void consume(IntResult1 res) {
        MyObject lo = po;
        if (lo != null) {
            res.r1 = lo.x;
        } else {
            res.r1 = -1;
        }
    }

    static class MyObject {
        int x;
    }

}
