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
import org.openjdk.jcstress.infra.results.II_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest
@State
@Outcome(id = {"0, 0", "1, 1"}, expect = ACCEPTABLE,             desc = "Boring")
@Outcome(id = "0, 1",           expect = ACCEPTABLE,             desc = "In order")
@Outcome(id = "1, 0",           expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
public class AdvancedJMM_01_SynchronizedBarriers {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_01
     */

    /*
      ----------------------------------------------------------------------------------------------------------

         This is the first example that shows the advanced things in JMM. Most of these examples show
         what JMM is *not*, rather that what JMM *is*.

         It is easy to read "JSR 133 Cookbook for Compiler Writers" and get a wrong idea that the conservative
         implementation that JSR 133 Cookbook provides as the implementation guidance is the memory
         model as specified. For example, it is a common mistake to assume that synchronized blocks
         have barriers associated with them.

         In this example, we do two back-to-back synchronized blocks in one thread, and read the updates
         in the other thread with the maximum ordering possible, "volatile". If synchronized blocks indeed
         provided the barriers, then it would not be possible to write "x" and "y" out of order, and
         therefore, "1, 0" outcome would be forbidden.

         But in reality, even on x86_64, this test yields:

              RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
                0, 0  3,815,999,238   92.96%   Acceptable  Boring
                0, 1     10,345,809    0.25%   Acceptable  In order
                1, 0        207,479   <0.01%  Interesting  Whoa
                1, 1    278,646,578    6.79%   Acceptable  Boring

         Technically, this is due to "lock coarsening" that merged the synchronized blocks, and then was able
         to order the writes to "x" and "y" differently. JMM as stated allows
         this optimization: we are only required to see these stores in order if we are
         synchronizing on the same "this". Side observers can see the writes in whatever order.
    */

    static final VarHandle VH_X, VH_Y;

    static {
        try {
            VH_X = MethodHandles.lookup().findVarHandle(AdvancedJMM_01_SynchronizedBarriers.class, "x", int.class);
            VH_Y = MethodHandles.lookup().findVarHandle(AdvancedJMM_01_SynchronizedBarriers.class, "y", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    int x, y;

    @Actor
    void actor() {
        synchronized (this) {
            x = 1;
        }
        synchronized (this) {
            y = 1;
        }
    }

    @Actor
    void observer(II_Result r) {
        r.r1 = (int) VH_Y.getVolatile(this);
        r.r2 = (int) VH_X.getVolatile(this);
    }
}