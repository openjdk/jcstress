/*
 * Copyright (c) 2019, Red Hat Inc. All rights reserved.
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
package org.openjdk.jcstress.tests.collections;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.IIII_Result;

import java.util.HashMap;
import java.util.Map;

@JCStressTest
@Outcome(id = "0, 0, 1, 2", expect = Expect.ACCEPTABLE, desc = "No exceptions, entire map is okay.")
@Outcome(expect = Expect.ACCEPTABLE_INTERESTING, desc = "Something went wrong")
@State
public class HashMapFailureTest {

    private final Map<Integer, Integer> map = new HashMap<>();

    @Actor
    public void actor1(IIII_Result r) {
        try {
            map.put(1, 1);
            r.r1 = 0;
        } catch (Exception e) {
            r.r1 = 1;
        }
    }

    @Actor
    public void actor2(IIII_Result r) {
        try {
            map.put(2, 2);
            r.r2 = 0;
        } catch (Exception e) {
            r.r2 = 1;
        }
    }

    @Arbiter
    public void arbiter(IIII_Result r) {
        Integer v1 = map.get(1);
        Integer v2 = map.get(2);
        r.r3 = (v1 != null) ? v1 : -1;
        r.r4 = (v2 != null) ? v2 : -1;
    }

}
