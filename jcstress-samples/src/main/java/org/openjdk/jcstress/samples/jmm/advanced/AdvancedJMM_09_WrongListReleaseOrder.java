/*
 * Copyright (c) 2016, 2021, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jcstress.samples.jmm.advanced;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.ArrayList;
import java.util.List;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest
@State
@Outcome(id = {"-1", "42"},       expect = ACCEPTABLE,             desc = "Boring")
@Outcome(id = {"-2", "-3", "-4"}, expect = ACCEPTABLE_INTERESTING, desc = "Whoa-whoa")
public class AdvancedJMM_09_WrongListReleaseOrder {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_09_WrongListReleaseOrder
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        As the extension of the previous example, in most practical cases, the wrong release order manifests
        like the premature publication on the mutating objects, for example a collection. This test initializes
        and stores/releases the initial list before doing the write. Even though the list is "volatile", the
        addition happens late, and the proper "publication" does not help.

        x86_64:
          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
              -1  9,508,599,695   81.61%   Acceptable  Boring
              -2     28,299,861    0.24%  Interesting  Whoa-whoa
              -3              0    0.00%  Interesting  Whoa-whoa
              -4          7,308   <0.01%  Interesting  Whoa-whoa
              42  2,114,748,816   18.15%   Acceptable  Boring

        AArch64:
          RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
              -1  519,561,253   82.38%   Acceptable  Boring
              -2    1,241,751    0.20%  Interesting  Whoa-whoa
              -3        1,902   <0.01%  Interesting  Whoa-whoa
              -4            9   <0.01%  Interesting  Whoa-whoa
              42  109,901,261   17.43%   Acceptable  Boring

        The "-1" outcome is very visible for obvious reasons, it is explainable by sequential execution.

        The "-2" and "-3" outcomes are possible due to simple visibility failures: the store
        to backing array is not yet visible.

        There is also a very interesting "-4" outcome, which shows that mutating collections under
        races is not a good idea. Even though isEmpty() returned "false" and we proceeded to "get",
        the internal checks in "get" read the sizes again and discovered the list is not yet initialized
        (coherence failure).
     */

    volatile List<Integer> list;

    @Actor
    public void actor1() {
        list = new ArrayList<>(); // prematurely released
        list.add(42);
    }

    @Actor
    public void actor2(I_Result r) {
        List<Integer> l = list;
        if (l == null) {
            r.r1 = -1;
            return;
        }

        if (l.isEmpty()) {
            r.r1 = -2;
            return;
        }

        try {
            Integer li = l.get(0);
            if (li == null) {
                r.r1 = -3;
                return;
            }
            r.r1 = li;
        } catch (ArrayIndexOutOfBoundsException e) {
            r.r1 = -4;
        }
    }
}
