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

package org.openjdk.jcstress.properties;

import org.openjdk.jcstress.Options;

import java.util.Arrays;
import java.util.List;

class TestTimeProperties implements UsedProperties.ProeprtiesHelpProvider {

    static final UsedProperties.IntJcstressProperty TIMEBUDGET_DEFAULTPERTESTMS = new UsedProperties.IntJcstressProperty("jcstress.timeBudget.defaultPerTestMs", 3000) {
        @Override
        public String getDescription() {
            return getKey() + " set default time the individual test executes. Defaults to " + getDefault() +
                    "ms set to " + getValue() + "ms";
        }
    };

    static final UsedProperties.IntJcstressProperty TIMEBUDGET_MINTIMEMS = new UsedProperties.IntJcstressProperty("jcstress.timeBudget.minTimeMs", 30) {
        @Override
        public String getDescription() {
            return getKey() + " set minimal time the individual test executes. Defaults to " + getDefault() +
                    "ms set to " + getValue() + "ms";
        }
    };

    static final UsedProperties.IntJcstressProperty TIMEBUDGET_MAXTIMEMS = new UsedProperties.IntJcstressProperty("jcstress.timeBudget.maxTimeMs", 60_000) {
        @Override
        public String getDescription() {
            return getKey() +
                    " set maximum time the individual test executes. Defaults to " + getDefault() +
                    "ms set to " + getValue() + "ms";
        }
    };

    static final String TIMEBUDGET_ADDITIONAL_COMMENT = "The time each test is run is (simplified) calculated as value of " + Options.TIME_BUDGET_SWITCH + " switch divided by number of tests (after all filters applied)" +
            " If te resulting time is smaller then " + TIMEBUDGET_MINTIMEMS.getKey() + ", it is used.  If it si bigger then " + TIMEBUDGET_MAXTIMEMS.getKey() + " it is used. If no " + Options.TIME_BUDGET_SWITCH + "  is set," +
            " then " + TIMEBUDGET_DEFAULTPERTESTMS.getKey() + " is used. See " + Options.TIME_BUDGET_SWITCH + " for more info. Properties do not accept unit suffixes.";

    @Override
    public List<String> getHelp() {
        return Arrays.asList(
                TIMEBUDGET_DEFAULTPERTESTMS.getDescription(),
                TIMEBUDGET_MINTIMEMS.getDescription(),
                TIMEBUDGET_MAXTIMEMS.getDescription(),
                TIMEBUDGET_ADDITIONAL_COMMENT);
    }

    @Override
    public String getTitle() {
        return "Individual test time execution properties";
    }
}
