/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress;

import org.openjdk.jcstress.infra.runners.TestConfig;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class TestListing {

    public static final String FLAT_JSON_VARIANTS = "jcstress.list.json.flat";

    public enum ListingTypes {
        NONE, ALL, ALL_MATCHING, ALL_MATCHING_COMBINATIONS,
        MATCHING_GROUPS, MATCHING_GROUPS_COUNT,
        MATCHING_IGROUPS, MATCHING_IGROUPS_COUNT,
        TOTAL_ALL, TOTAL_ALL_MATCHING, TOTAL_ALL_MATCHING_COMBINATIONS,
        TOTAL_MATCHING_GROUPS, TOTAL_MATCHING_GROUPS_COUNT,
        TOTAL_MATCHING_IGROUPS, TOTAL_MATCHING_IGROUPS_COUNT,
        JSON_ALL, JSON_ALL_MATCHING, JSON_ALL_MATCHING_COMBINATIONS,
        JSON_MATCHING_GROUPS, JSON_MATCHING_GROUPS_COUNT,
        JSON_MATCHING_IGROUPS, JSON_MATCHING_IGROUPS_COUNT;

        public static String toDescription() {
            return "Optional parameter is: "
                    + ALL + " all tests; "
                    + ALL_MATCHING + " all tests eligible for this system and configuration (like CPU count); "
                    + ALL_MATCHING_COMBINATIONS + " all real combinations which will run in this setup; "
                    + MATCHING_GROUPS + " similar to above but the shared part is printed only once; "
                    + MATCHING_GROUPS_COUNT + " same as above, only instead of lsiting, just count is used; "
                    + MATCHING_IGROUPS + ", " + MATCHING_IGROUPS_COUNT + " same as above, only inverted; "
                    + "Defaults to " + ALL_MATCHING + " if none provided. You can prefix by TOTAL_ or JSON_"
                    + "to print only summary line or to print valid jsons";
        }
    }

    private final JCStress jcstress;

    public TestListing(JCStress jcstress) {
        this.jcstress = jcstress;
    }

    @SuppressWarnings("unchecked")
    public int listTests() {
        JCStress.ConfigsWithScheduler configsWithScheduler = jcstress.getConfigs();
        Map<String, Object> testsToPrint = new TreeMap<>();
        switch (jcstress.opts.listingType()) {
            case ALL_MATCHING_COMBINATIONS:
            case TOTAL_ALL_MATCHING_COMBINATIONS:
            case JSON_ALL_MATCHING_COMBINATIONS:
                for (TestConfig test : configsWithScheduler.configs) {
                    //in json mode, see FIXME lower
                    testsToPrint.put(test.toDetailedTest(), null);
                }
                jcstress.out.println("All matching tests combinations - " + testsToPrint.size());
                break;
            case MATCHING_GROUPS_COUNT:
            case TOTAL_MATCHING_GROUPS_COUNT:
            case JSON_MATCHING_GROUPS_COUNT:
                for (TestConfig test : configsWithScheduler.configs) {
                    //in json mode, see FIXME lower
                    Integer counter = (Integer) testsToPrint.getOrDefault(test.getTestVariant(false), 0);
                    counter++;
                    testsToPrint.put(test.getTestVariant(false), counter);
                }
                jcstress.out.println("All existing combinations (each with count of test) " + testsToPrint.size());
                break;
            case MATCHING_IGROUPS_COUNT:
            case TOTAL_MATCHING_IGROUPS_COUNT:
            case JSON_MATCHING_IGROUPS_COUNT:
                for (TestConfig test : configsWithScheduler.configs) {
                    Integer counter = (Integer) testsToPrint.getOrDefault(test.name, 0);
                    counter++;
                    testsToPrint.put(test.name, counter);
                }
                jcstress.out.println("All matching tests (each with count of combinations) " + testsToPrint.size());
                break;
            case MATCHING_GROUPS:
            case TOTAL_MATCHING_GROUPS:
            case JSON_MATCHING_GROUPS:
                for (TestConfig test : configsWithScheduler.configs) {
                    Set<String> items = (Set<String>) testsToPrint.getOrDefault(test.getTestVariant(false), new TreeSet<String>());
                    //in json mode, see FIXME lower
                    items.add(test.name);
                    testsToPrint.put(test.getTestVariant(false), items);
                }
                jcstress.out.println("All existing combinations " + testsToPrint.size());
                break;
            case MATCHING_IGROUPS:
            case TOTAL_MATCHING_IGROUPS:
            case JSON_MATCHING_IGROUPS:
                for (TestConfig test : configsWithScheduler.configs) {
                    Set<String> items = (Set<String>) (testsToPrint.getOrDefault(test.name, new TreeSet<String>()));
                    items.add(test.getTestVariant(false));
                    testsToPrint.put(test.name, items);
                }
                jcstress.out.println("All matching tests " + testsToPrint.size());
                break;
            case ALL_MATCHING:
            case TOTAL_ALL_MATCHING:
            case JSON_ALL_MATCHING:
                for (TestConfig test : configsWithScheduler.configs) {
                    testsToPrint.put(test.name, null);
                }
                jcstress.out.println("All matching tests - " + testsToPrint.size());
                break;
            case ALL:
            case TOTAL_ALL:
            case JSON_ALL:
                for (String test : jcstress.getTests()) {
                    testsToPrint.put(test, null);
                }
                jcstress.out.println("All existing tests combinations - " + testsToPrint.size());
                break;
            default:
                throw new RuntimeException("Invalid option for listing: " + jcstress.opts.listingType());
        }
        if (jcstress.opts.listingType().toString().startsWith("TOTAL_")) {
            return testsToPrint.size();
        }
        if (jcstress.opts.listingType().toString().startsWith("JSON_")) {
            jcstress.out.println("{");
            jcstress.out.println("\"toal\": " + testsToPrint.size() + ", \"list\": [");
        }
        Set<Map.Entry<String, Object>> entries = testsToPrint.entrySet();
        int counter = entries.size();
        for (Map.Entry<String, Object> test : entries) {
            counter--;
            if (test.getValue() == null) {
                if (jcstress.opts.listingType().toString().startsWith("JSON_")) {
                    jcstress.out.print("\"" + test.getKey() + "\"");
                    jsonArrayDelimiter(counter);
                } else {
                    jcstress.out.println(test.getKey());
                }
            } else {
                if (test.getValue() instanceof Integer) {
                    //"[publish, consume], spinLoopStyle: Thread.onSpinWait(), threads: 2, forkId: 2, maxFootprintMB: 64, compileMode: 8, shClass: (PG 0, CG 0), (PG 0, CG 1), strideSize: 256, strideCount: 40, cpuMap: null, [-XX:+UseBiasedLocking, -XX:+StressLCM, -XX:+StressGCM, -XX:+StressIGVN,
                    // -XX:+StressCCP, -XX:StressSeed=yyyyyyyy]": 1
                    //x
                    // "org.openjdk.jcstress.tests.unsafe.UnsafeAddLong1": 96
                    //FIXME, refactor the first, long one, so individual parts are json elements
                    if (jcstress.opts.listingType().toString().startsWith("JSON_")) {
                        jcstress.out.println("{\"" + test.getKey() + "\": " + test.getValue() + "}");
                        jsonArrayDelimiter(counter);
                    } else {
                        jcstress.out.println(test.getValue() + " " + test.getKey());
                    }
                } else if (test.getValue() instanceof Collection) {
                    //FIXME, same as above
                    if (jcstress.opts.listingType().toString().startsWith("JSON_")) {
                        jcstress.out.println("{\"" + test.getKey() + "\": [");
                        int subcounter = ((Collection) test.getValue()).size();
                        for (Object item : (Collection) test.getValue()) {
                            subcounter--;
                            jcstress.out.println("\"" + item + "\"");
                            jsonArrayDelimiter(subcounter);
                        }
                        jcstress.out.println("]}");
                        jsonArrayDelimiter(counter);
                    } else {
                        jcstress.out.println(test.getKey() + " " + ((Collection) test.getValue()).size());
                        for (Object item : (Collection) test.getValue()) {
                            jcstress.out.println("    " + item);
                        }
                    }
                } else {
                    jcstress.out.println(test.getKey() + "=?=" + test.getValue());
                }
            }
        }
        if (jcstress.opts.listingType().toString().startsWith("JSON_")) {
            jcstress.out.println("]}");
        }
        return testsToPrint.size();
    }

    private void jsonArrayDelimiter(int counter) {
        if (counter != 0) {
            jcstress.out.println(",");
        } else {
            jcstress.out.println();
        }
    }

}
