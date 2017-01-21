/*
 * Copyright (c) 2016, Red Hat Inc.
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
package org.openjdk.jcstress.samples;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.StringResult2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.openjdk.jcstress.annotations.Expect.*;

public class ConcurrencySample_02_ConcurrentHashMap {

    /*
      ----------------------------------------------------------------------------------------------------------

        This test demonstrates the operation atomicity tests, taking
        ConcurrentHashMap-backed Multimap as the example.
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

              [OK] org.openjdk.jcstress.samples.ConcurrencySample_02_ConcurrentHashMap.BrokenMultimap
            (JVM args: [-server])
          Observed state   Occurrences              Expectation  Interpretation
                Bar, Baz     5,635,469               ACCEPTABLE  Both updates.
               Bar, null     1,691,183   ACCEPTABLE_INTERESTING  One update lost.
                Baz, Bar     5,821,971               ACCEPTABLE  Both updates.
               Baz, null     1,690,007   ACCEPTABLE_INTERESTING  One update lost.
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
        public void arbiter(StringResult2 s) {
            s.r1 = poll("Foo", 0);
            s.r2 = poll("Foo", 1);
        }
    }

    /*
       ----------------------------------------------------------------------------------------------------------

        putIfAbsent-style multimap does atomic updates.

              [OK] org.openjdk.jcstress.samples.ConcurrencySample_02_ConcurrentHashMap.CorrectMultimap
            (JVM args: [-server])
          Observed state   Occurrences   Expectation  Interpretation
                Bar, Baz     6,948,547    ACCEPTABLE  Both updates.
               Bar, null             0     FORBIDDEN  One update lost.
                Baz, Bar     7,360,653    ACCEPTABLE  Both updates.
               Baz, null             0     FORBIDDEN  One update lost.
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
        public void arbiter(StringResult2 s) {
            s.r1 = poll("Foo", 0);
            s.r2 = poll("Foo", 1);
        }
    }




    /*
       ----------------------------------------------------------------------------------------------------------

        computeIfAbsent-style multimap does atomic updates.

              [OK] org.openjdk.jcstress.samples.ConcurrencySample_02_ConcurrentHashMap.CorrectJDK8Multimap
            (JVM args: [-server])
          Observed state   Occurrences   Expectation  Interpretation
                Bar, Baz     6,250,933    ACCEPTABLE  Both updates.
               Bar, null             0     FORBIDDEN  One update lost.
                Baz, Bar     6,412,677    ACCEPTABLE  Both updates.
               Baz, null             0     FORBIDDEN  One update lost.
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
        public void arbiter(StringResult2 s) {
            s.r1 = poll("Foo", 0);
            s.r2 = poll("Foo", 1);
        }
    }

}
