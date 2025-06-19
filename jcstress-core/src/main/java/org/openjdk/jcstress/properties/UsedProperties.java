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


import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.openjdk.jcstress.properties.TestTimeProperties.*;
import static org.openjdk.jcstress.properties.ForkedVmProperties.*;
import static org.openjdk.jcstress.properties.UiProperties.*;

/**
 * Known Properties affecting JCStress. Some of them publicly document themselves, some do not need to.
 */
public class UsedProperties {

    interface ProeprtiesHelpProvider {
        List<String> getHelp();

        String getTitle();
    }

    static abstract class JcstressProperty<T> {
        private final String key;
        private final List<T> defaults;

        public JcstressProperty(String key, T[] defaults) {
            this.key = key;
            this.defaults = Collections.unmodifiableList(Arrays.asList(defaults));
        }

        public String getKey() {
            return key;
        }

        public List<T> getDefaults() {
            return defaults;
        }

        public T getDefault() {
            return defaults.get(0);
        }

        public boolean isCustom() {
            return System.getProperty(getKey()) != null;
        }

        public abstract String getDescription();

        public abstract T getValue();
    }

    static abstract class IntJcstressProperty extends JcstressProperty<Integer> {

        public IntJcstressProperty(String key, Integer... defaults) {
            super(key, defaults);
        }

        @Override
        public Integer getValue() {
            return Integer.getInteger(getKey(), getDefault());
        }
    }

    static abstract class LongJcstressProperty extends JcstressProperty<Long> {

        public LongJcstressProperty(String key, Long... defaults) {
            super(key, defaults);
        }

        @Override
        public Long getValue() {
            return Long.getLong(getKey(), getDefault());
        }
    }


    static abstract class StringJcstressProperty extends JcstressProperty<String> {

        public StringJcstressProperty(String key, String... defaults) {
            super(key, defaults);
        }

        @Override
        public String getValue() {
            return System.getProperty(getKey(), getDefault());
        }
    }

    /**
     * utility class
     */
    private UsedProperties() {
    }

    public static int getJcstressTimeBudgetDefaultPerTestMs() {
        return TIMEBUDGET_DEFAULTPERTESTMS.getValue();
    }

    public static int getJcstressTimeBudgetMinTimeMs() {
        return TIMEBUDGET_MINTIMEMS.getValue();
    }

    public static int getJcstressTimeBudgetMaxTimeMs() {
        return TIMEBUDGET_MAXTIMEMS.getValue();
    }

    private static String getJcstressLinkAddress() {
        return LINK_ADDRESS.getValue();
    }

    public static InetAddress getListenAddress() {
        // Try to use user-provided override first.
        if (UsedProperties.getJcstressLinkAddress() != null) {
            try {
                return InetAddress.getByName(UsedProperties.getJcstressLinkAddress());
            } catch (UnknownHostException e) {
                // override failed, notify user
                throw new IllegalStateException("Can not initialize binary link.", e);
            }
        }

        return InetAddress.getLoopbackAddress();
    }


    public static int getJcstressLinkPort() {
        return LINK_PORT.getValue();
    }


    public static int getJcstressLinkTimeoutMs() {
        return LINK_TIMEOUTMS.getValue();
    }

    public static boolean isProgressInteractive() {
        return System.console() != null;
    }

    public static long getPrintIntervalMs() {
        return (CONSOLE_PRINTINTERVALMS.isCustom()) ?
                CONSOLE_PRINTINTERVALMS.getValue() :
                isProgressInteractive() ? CONSOLE_PRINTINTERVALMS.getDefaults().get(0) : CONSOLE_PRINTINTERVALMS.getDefaults().get(1);
    }

    public static void printHelpOn(PrintStream ouer) {
        ouer.println("JCStress recognize several internal properties:");
        for (ProeprtiesHelpProvider provider : new ProeprtiesHelpProvider[]{new TestTimeProperties(), new ForkedVmProperties(), new UiProperties()}) {
            ouer.println(" " + provider.getTitle());
            for (String line : provider.getHelp()) {
                ouer.println(" * " + line);
            }
        }
    }
}