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

import java.util.Arrays;
import java.util.List;

class UiProperties implements UsedProperties.ProeprtiesHelpProvider {

    static final UsedProperties.LongJcstressProperty CONSOLE_PRINTINTERVALMS = new UsedProperties.LongJcstressProperty("jcstress.console.printIntervalMs", 1_000L, 15_000L) {

        @Override
        public Long getValue() {
            return Long.getLong(getKey(), null);
        }

        @Override
        public String getDescription() {
            return getKey() + " sets interval how often to print results to console in ms. Have two defaults: " +
                    getDefaults().get(0) + "ms in interactive mode and " +
                    getDefaults().get(1) + "ms in noninteractive mode. Set to " +
                    UsedProperties.getPrintIntervalMs() + "ms";
        }
    };

    @Override
    public List<String> getHelp() {
        return Arrays.asList(CONSOLE_PRINTINTERVALMS.getDescription());
    }

    @Override
    public String getTitle() {
        return "Properties modyfying user appearance";
    }
}
