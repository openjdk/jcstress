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
package org.openjdk.jcstress.samples.primitives.library;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.LL_Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.openjdk.jcstress.annotations.Expect.*;

public class Library_01_CHM {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t Library_01_CHM
    */

    /*
      ----------------------------------------------------------------------------------------------------------
        This test demonstrates the operation atomicity tests, taking ConcurrentHashMap-backed
        Multimap as the example.
     */

    public static class Multimap {
        Map<String, List<String>> map = new ConcurrentHashMap<>();

        /*
            Contains a nasty race on putting the list into the map.
         */
        void addBroken(String key, String val) {
            List<String> list = map.get(key);
            if (list == null) {
                list = Collections.synchronizedList(new ArrayList<>());
                map.put(key, list);
            }
            list.add(val);
        }

        /*
            Solves the race with putIfAbsent.
         */
        void addCorrect(String key, String val) {
            List<String> list = map.get(key);
            if (list == null) {
                list = Collections.synchronizedList(new ArrayList<>());
                List<String> exist = map.putIfAbsent(key, list);
                if (exist != null) {
                    list = exist;
                }
            }
            list.add(val);
        }

        /*
            Solves the race with computeIfAbsent.
         */
        void addCorrect8(String key, String val) {
            List<String> list = map.computeIfAbsent(key,
                    k -> Collections.synchronizedList(new ArrayList<>()));
            list.add(val);
        }

        String poll(String key, int idx) {
            List<String> list = map.get(key);
            return (list.size() > idx) ? list.get(idx) : null;
        }
    }

    /*
       ----------------------------------------------------------------------------------------------------------

        Broken Multimap is broken, it contains a race.

             RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
           Bar, Baz  110,869,386   42.56%   Acceptable  Both updates.
          Bar, null   20,165,976    7.74%  Interesting  One update lost.
           Baz, Bar  109,309,826   41.96%   Acceptable  Both updates.
          Baz, null   20,153,756    7.74%  Interesting  One update lost.
     */

    @JCStressTest
    @Outcome(id = { "Bar, null", "Baz, null" }, expect = ACCEPTABLE_INTERESTING, desc = "One update lost.")
    @Outcome(id = { "Bar, Baz", "Baz, Bar"},    expect = ACCEPTABLE, desc = "Both updates.")
    @State
    public static class BrokenMultimap extends Multimap {
        @Actor
        public void actor1() {
            addBroken("Foo", "Bar");
        }

        @Actor
        public void actor2() {
            addBroken("Foo", "Baz");
        }

        @Arbiter
        public void arbiter(LL_Result s) {
            s.r1 = poll("Foo", 0);
            s.r2 = poll("Foo", 1);
        }
    }

    /*
       ----------------------------------------------------------------------------------------------------------

        putIfAbsent-style multimap does atomic updates.

             RESULT      SAMPLES     FREQ      EXPECT  DESCRIPTION
           Bar, Baz  125,206,656   50.51%  Acceptable  Both updates.
          Bar, null            0    0.00%   Forbidden  One update lost.
           Baz, Bar  122,666,368   49.49%  Acceptable  Both updates.
          Baz, null            0    0.00%   Forbidden  One update lost.
     */


    @JCStressTest
    @Outcome(id = { "Bar, null", "Baz, null" }, expect = FORBIDDEN, desc = "One update lost.")
    @Outcome(id = { "Bar, Baz", "Baz, Bar"},    expect = ACCEPTABLE, desc = "Both updates.")
    @State
    public static class CorrectMultimap extends Multimap {
        @Actor
        public void actor1() {
            addCorrect("Foo", "Bar");
        }

        @Actor
        public void actor2() {
            addCorrect("Foo", "Baz");
        }

        @Arbiter
        public void arbiter(LL_Result s) {
            s.r1 = poll("Foo", 0);
            s.r2 = poll("Foo", 1);
        }
    }


    /*
       ----------------------------------------------------------------------------------------------------------

        computeIfAbsent-style multimap does atomic updates.

             RESULT     SAMPLES     FREQ      EXPECT  DESCRIPTION
           Bar, Baz  97,992,669   49.72%  Acceptable  Both updates.
          Bar, null           0    0.00%   Forbidden  One update lost.
           Baz, Bar  99,110,435   50.28%  Acceptable  Both updates.
          Baz, null           0    0.00%   Forbidden  One update lost.
     */

    @JCStressTest
    @Outcome(id = { "Bar, null", "Baz, null" }, expect = FORBIDDEN, desc = "One update lost.")
    @Outcome(id = { "Bar, Baz", "Baz, Bar"},    expect = ACCEPTABLE, desc = "Both updates.")
    @State
    public static class CorrectJDK8Multimap extends Multimap {
        @Actor
        public void actor1() {
            addCorrect8("Foo", "Bar");
        }

        @Actor
        public void actor2() {
            addCorrect8("Foo", "Baz");
        }

        @Arbiter
        public void arbiter(LL_Result s) {
            s.r1 = poll("Foo", 0);
            s.r2 = poll("Foo", 1);
        }
    }

}
