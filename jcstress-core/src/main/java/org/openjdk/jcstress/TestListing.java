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

    public enum ListingTypes {
        NONE, ALL, ALL_MATCHING, ALL_MATCHING_COMBINATIONS,
        MATCHING_GROUPS, MATCHING_GROUPS_COUNT,
        MATCHING_IGROUPS, MATCHING_IGROUPS_COUNT;

        public static String toDescription() {
            return "Optional parameter is: "
                    + ALL + " - all tests; "
                    + ALL_MATCHING + " all tests eligible for this system and configuration (like CPU count); "
                    + ALL_MATCHING_COMBINATIONS + " all real combinations which will run in this setup."
                    + MATCHING_GROUPS + " similar to above but the shared part is printed only once"
                    + MATCHING_GROUPS_COUNT + " same as above, only instead of lsiting, just count is used"
                    + MATCHING_IGROUPS + ", " + MATCHING_IGROUPS_COUNT + " same as above, only inverted"
                    + " Defaults to " + ALL_MATCHING + " if none provided.";
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
                for (TestConfig test : configsWithScheduler.configs) {
                    testsToPrint.put(test.toDetailedTest(), null);
                }
                jcstress.out.println("All matching tests combinations - " + testsToPrint.size());
                break;
            case MATCHING_GROUPS_COUNT:
                for (TestConfig test : configsWithScheduler.configs) {
                    Integer counter = (Integer) testsToPrint.getOrDefault(test.getTestVariant(false), 0);
                    counter++;
                    testsToPrint.put(test.getTestVariant(false), counter);
                }
                jcstress.out.println("All existing combinations (each with count of test) " + testsToPrint.size());
                break;
            case MATCHING_IGROUPS_COUNT:
                for (TestConfig test : configsWithScheduler.configs) {
                    Integer counter = (Integer) testsToPrint.getOrDefault(test.name, 0);
                    counter++;
                    testsToPrint.put(test.name, counter);
                }
                jcstress.out.println("All matching tests (each with count of combinations) " + testsToPrint.size());
                break;
            case MATCHING_GROUPS:
                for (TestConfig test : configsWithScheduler.configs) {
                    Set<String> items = (Set<String>) testsToPrint.getOrDefault(test.getTestVariant(false), new TreeSet<String>());
                    items.add(test.name);
                    testsToPrint.put(test.getTestVariant(false), items);
                }
                jcstress.out.println("All existing combinations " + testsToPrint.size());
                break;
            case MATCHING_IGROUPS:
                for (TestConfig test : configsWithScheduler.configs) {
                    Set<String> items = (Set<String>) (testsToPrint.getOrDefault(test.name, new TreeSet<String>()));
                    items.add(test.getTestVariant(false));
                    testsToPrint.put(test.name, items);
                }
                jcstress.out.println("All matching tests" + testsToPrint.size());
                break;
            case ALL_MATCHING:
                for (TestConfig test : configsWithScheduler.configs) {
                    testsToPrint.put(test.name, null);
                }
                jcstress.out.println("All matching tests - " + testsToPrint.size());
                break;
            case ALL:
                for (String test : jcstress.getTests()) {
                    testsToPrint.put(test, null);
                }
                jcstress.out.println("All existing tests combinations - " + testsToPrint.size());
                break;
            default:
                throw new RuntimeException("Invalid option for listing: " + jcstress.opts.listingType());
        }
        for (Map.Entry<String, Object> test : testsToPrint.entrySet()) {
            if (test.getValue() == null) {
                jcstress.out.println(test.getKey());
            } else {
                if (test.getValue() instanceof Integer) {
                    jcstress.out.println(test.getValue() + " " + test.getKey());
                } else if (test.getValue() instanceof Collection) {
                    jcstress.out.println(test.getKey() + " " + ((Collection) test.getValue()).size());
                    for (Object item : (Collection) test.getValue()) {
                        jcstress.out.println("    " + item);
                    }
                } else {
                    jcstress.out.println(test.getKey() + "=?=" + test.getValue());
                }
            }
        }
        return testsToPrint.size();
    }

}
