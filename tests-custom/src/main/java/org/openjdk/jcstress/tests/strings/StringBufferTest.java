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
package org.openjdk.jcstress.tests.strings;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.StringResult1;

@JCStressTest
@Description("Tests the StringBuffers are working good under concurrent updates.")
@Outcome(id = "b{4}", expect = Expect.ACCEPTABLE, desc = "All appends are visible")
@State
public class StringBufferTest {

    StringBuffer sb = new StringBuffer(0);

    @Actor
    public void actor1() {
        for (int i = 0; i < 2; ++i) {
            sb.append('b');
        }
    }

    @Actor
    public void actor2() {
        for (int i = 0; i < 2; ++i) {
            sb.append('b');
        }
    }

    @Arbiter
    public void tester(StringResult1 r) {
        r.r1 = sb.toString();
    }

}
