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

import static org.openjdk.jcstress.annotations.Expect.*;

public class AdvancedJMM_06_SemiSynchronized {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_06_SemiSynchronized[.SubTestName]
     */

    static class Composite {
        int x1, x2, x3, x4;
        public Composite(int v) {
            x1 = v; x2 = v; x3 = v; x4 = v;
        }
        public int get() {
            return x4 + x3 + x2 + x1;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Somewhat similar to previous example, this test now exchanges the Composite directly between the
        threads. In this example, we synchronize the reader. But the writer is deliberately non-synchronized.
        Unfortunately, synchronizing only the reader part is not enough: the writer is still racy, and can
        perform the writes in whatever order, thus revealing the surprising results to the casual observer.

        This can be seen on some platforms, for example AArch64, PPC64, and it becomes even more visible
        with -XX:+UseBiasedLocking:

          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
              -1  2,777,131,721   83.31%   Acceptable  Boring
               0          1,536   <0.01%  Interesting  Whoa
               1             52   <0.01%  Interesting  Whoa
               2             23   <0.01%  Interesting  Whoa
               3             67   <0.01%  Interesting  Whoa
               4    556,205,737   16.69%   Acceptable  Boring
     */

    @JCStressTest
    @State
    @Outcome(id = {"-1", "4"}, expect = ACCEPTABLE,             desc = "Boring")
    @Outcome(                  expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
    public static class RacyWrite {
        Composite c;

        @Actor
        void writer() {
            c = new Composite(1);
        }

        @Actor
        void reader(I_Result r) {
            synchronized (this) {
                Composite lc = c;
                r.r1 = (lc != null) ? lc.get() : -1;
            }
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        Nothing really changes if we synchronize writer instead of reader. What happens within the synchronized
        block can be the same as in the example above, and reader would observe it, regardless the synchronization
        block, since it does not synchronize with the writer.

        Again, on AArch64 and PPC64:
          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
              -1  2,986,368,606   89.69%   Acceptable  Boring
               0          2,118   <0.01%  Interesting  Whoa
               1            121   <0.01%  Interesting  Whoa
               2             51   <0.01%  Interesting  Whoa
               3            161   <0.01%  Interesting  Whoa
               4    343,332,879   10.31%   Acceptable  Boring
     */

    @JCStressTest
    @State
    @Outcome(id = {"-1", "4"}, expect = ACCEPTABLE,             desc = "Boring")
    @Outcome(                  expect = ACCEPTABLE_INTERESTING, desc = "Whoa")
    public static class RacyRead {
        Composite c;

        @Actor
        void writer() {
            synchronized (this) {
                c = new Composite(1);
            }
        }

        @Actor
        void reader(I_Result r) {
            Composite lc = c;
            r.r1 = (lc != null) ? lc.get() : -1;
        }
    }

    /*
      ----------------------------------------------------------------------------------------------------------

        If we properly synchronize both reader and writer parts, the previously interesting
        outcomes are now forbidden.

        AArch64, PPC64:
          RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
              -1  927,516,147   50.71%  Acceptable  Boring
               4  901,720,589   49.29%  Acceptable  Boring
     */
    @JCStressTest
    @State
    @Outcome(id = {"-1", "4"}, expect = ACCEPTABLE, desc = "Boring")
    @Outcome(                  expect = FORBIDDEN,  desc = "Now forbidden")
    public static class NonRacy {
        Composite c;

        @Actor
        void actor() {
            synchronized (this) {
                c = new Composite(1);
            }
        }

        @Actor
        void observer(I_Result r) {
            synchronized (this) {
                Composite lc = c;
                r.r1 = (lc != null) ? lc.get() : -1;
            }
        }
    }

}