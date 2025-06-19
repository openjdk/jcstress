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

class ForkedVmProperties implements UsedProperties.ProeprtiesHelpProvider {

    static final UsedProperties.StringJcstressProperty LINK_ADDRESS = new UsedProperties.StringJcstressProperty("jcstress.link.address", new String[]{null}) {
        @Override
        public String getDescription() {
            return getKey() + " is address where to connect to forked VMs. Defaults to loop-back. Set to '" + getListenAddressForInfo() + "'";
        }
    };


    static final UsedProperties.IntJcstressProperty LINK_PORT = new UsedProperties.IntJcstressProperty("jcstress.link.port", 0) {
        @Override
        public String getDescription() {
            return getKey() + " is port where to connect to forked VMs on " + LINK_ADDRESS.getKey() + ". Defaults to " + getDefault() + " (random free port)." +
                    " Set to " + UsedProperties.getJcstressLinkPort();
        }
    };

    static final UsedProperties.IntJcstressProperty LINK_TIMEOUTMS = new UsedProperties.IntJcstressProperty("jcstress.link.timeoutMs", 30 * 1000) {
        @Override
        public String getDescription() {
            return getKey() + " set timeout to forked VM communication ms." +
                    " Defaults to " + getDefault() + "ms. Set to " + getValue() + "ms.";
        }
    };


    private static String getListenAddressForInfo() {
        try {
            return UsedProperties.getListenAddress().toString();
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public List<String> getHelp() {
        return Arrays.asList(
                LINK_ADDRESS.getDescription(),
                LINK_PORT.getDescription(),
                LINK_TIMEOUTMS.getDescription());
    }

    @Override
    public String getTitle() {
        return "Properties handling forked VMs. Modify only if you are sure what youa re doing";
    }

}
