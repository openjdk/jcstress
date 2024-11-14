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
import java.util.Set;
import java.util.TreeSet;

public class TestListing {

    public enum ListingTypes {
        NONE, ALL, ALL_MATCHING, ALL_MATCHING_COMBINATIONS;

        public static String toDescription() {
            return "Optional parameter is: "
                    + ALL + " - all tests; "
                    + ALL_MATCHING + " all tests eligible for this system and configuration (like CPU count); "
                    + ALL_MATCHING_COMBINATIONS + " all real combinations which will run in this setup." +
                    " Defaults to " + ALL_MATCHING + " if none provided.";
        }
    }

    private final JCStress jcstress;

    public TestListing(JCStress jcstress) {
        this.jcstress = jcstress;
    }

    public int listTests() {
        JCStress.ConfigsWithScheduler configsWithScheduler = jcstress.getConfigs();
        Set<String> testsToPrint = new TreeSet<>();
        switch (jcstress.opts.listingType()) {
            case ALL_MATCHING_COMBINATIONS:
                for (TestConfig test : configsWithScheduler.configs) {
                    testsToPrint.add(test.toDetailedTest());
                }
                jcstress.out.println("All matching tests combinations - " + testsToPrint.size());
                break;
            case ALL_MATCHING:
                for (TestConfig test : configsWithScheduler.configs) {
                    testsToPrint.add(test.name);
                }
                jcstress.out.println("All matching tests - " + testsToPrint.size());
                break;
            case ALL:
                for (String test : jcstress.getTests()) {
                    testsToPrint.add(test);
                }
                jcstress.out.println("All existing tests combinations - " + testsToPrint.size());
                break;
            default:
                throw new RuntimeException("Invalid option for listing: " + jcstress.opts.listingType());
        }
        for (String test : testsToPrint) {
            jcstress.out.println(test);
        }
        return testsToPrint.size();
    }

}
