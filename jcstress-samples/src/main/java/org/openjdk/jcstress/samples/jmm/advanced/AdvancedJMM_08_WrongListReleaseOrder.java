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
@Outcome(id = {"-1", "42"}, expect = ACCEPTABLE,             desc = "Boring")
@Outcome(id = "0",          expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
@Outcome(id = "-2",         expect = ACCEPTABLE_INTERESTING, desc = "Whoa-whoa")
public class AdvancedJMM_08_WrongListReleaseOrder {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_08_WrongListReleaseOrder[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        As the extension of the previous example, in most practical cases, the wrong release order manifests
        like the premature publication on the mutating objects, for example a collection. This test initializes
        and stores/releases the initial list before doing the write. Even though the list is "volatile", the
        addition happens late, and the proper "publication" does not help.

        This test on x86_64:
          RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
              -1            0    0.00%   Acceptable  Boring
              -2            1   <0.01%  Interesting  Whoa-whoa
               0  256,502,626   56.64%  Interesting  Whoa
              42  196,385,437   43.36%   Acceptable  Boring

        The "0" outcome is very visible for obvious reasons, it is explainable by sequential execution.

        There is also a very interesting "-1" outcome, which shows that mutating collections under
        races is not a good idea. Even though isEmpty() returned "false" and we proceeded to "get",
        the internal checks in "get" read the sizes again and discovered the list is not yet initialized
        (coherence failure).
     */

    volatile List<Integer> list = new ArrayList<>();

    @Actor
    public void actor1() {
        list = new ArrayList<>(); // prematurely released
        list.add(42);
    }

    @Actor
    public void actor2(I_Result r) {
        List<Integer> l = list;
        if (l != null) {
            if (l.isEmpty()) {
                r.r1 = 0;
            } else {
                try {
                    r.r1 = l.get(0);
                } catch (ArrayIndexOutOfBoundsException e) {
                    r.r1 = -2;
                }
            }
        } else {
            r.r1 = -1;
        }
    }
}