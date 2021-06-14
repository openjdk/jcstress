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
package org.openjdk.jcstress.samples.jmm.basic;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest
@Outcome(id = "0, 0", expect = ACCEPTABLE, desc = "The only valid result")
@Outcome(             expect = FORBIDDEN,  desc = "Every other result is forbidden")
@State
public class BasicJMM_10_OOTA {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t BasicJMM_10_OOTA
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        One of the most complicated topics is Out-Of-Thin-Air (OOTA) values. It has to do with behavior
        under the normally non-existent races in some corner cases. For example, the program below should
        only have (0, 0) as the result. The optimizers are not allowed to perform the speculative write
        of either (x = 1) or (y = 1) until the read of "x" are "y" are satisfied. Otherwise, that speculation
        can turn into the self-justifying prophecy, which would yield (1, 1).

        I.e. this transformation is illegal:

          if (x == 1) {            y = 1; // speculate
            y = 1;        ---->    if (x != 1) {
          }                          y = 0; // restore
                                   }

        If the transformation above happens, then there is a straighforward way to (1, 1):

                                      y = 1
          if (y == 1) { // reads 1
              x = 1;    // stores 1
          }
                                      if (x != 1) {
                                         y = 0; // does not happen
                                      }

        x86_64:
            RESULT         SAMPLES     FREQ      EXPECT  DESCRIPTION
              0, 0  12,126,918,144  100.00%  Acceptable  The only valid result
    */

    int x, y;

    @Actor
    public void thread1() {
        if (x == 1) {
            y = 1;
        }
    }

    @Actor
    public void thread2() {
        if (y == 1) {
            x = 1;
        }
    }

    @Arbiter
    public void check(II_Result r) {
        r.r1 = x;
        r.r2 = y;
    }

}
