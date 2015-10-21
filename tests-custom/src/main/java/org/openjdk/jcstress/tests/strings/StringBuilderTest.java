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
@Description("Tests the StringBuilders are working good under concurrent updates.")
@Outcome(id = "[bbbbbbbbbbbbbbbbbbbb]", expect = Expect.ACCEPTABLE, desc = "All appends are visible.")
@Outcome(expect = Expect.ACCEPTABLE_INTERESTING, desc = "Other values are expected, threads are messing with each other.")
@State
public class StringBuilderTest {

    StringBuilder sb = new StringBuilder(0);

    @Actor
    public void actor1() {
        try {
            for (int i = 0; i < 10; ++i) {
                sb.append('b');
            }
        } catch (Exception e) {
            // most probably AIOOBE, expected for this test
        }
    }

    @Actor
    public void actor2() {
        try {
            for (int i = 0; i < 10; ++i) {
                sb.append('b');
            }
        } catch (Exception e) {
            // most probably AIOOBE, expected for this test
        }
    }

    @Arbiter
    public void tester(StringResult1 r) {
        try {
            r.r1 = sb.toString();
        } catch (Exception e) {
            r.r1 = "<ERROR>";
        }
    }

}
