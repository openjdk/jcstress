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

import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Known Properties affecting JCStress. Some of them publicly document themselves, some do not need to.
 */
public class UsedProperties {

    private static abstract class JcstressProperty<T> {
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

        public boolean isCustom(){
            return System.getProperty(getKey()) != null;
        }

        public abstract String getDescription();

        public abstract T getValue();
    }

    private static abstract class IntJcstressProperty extends JcstressProperty<Integer> {

        public IntJcstressProperty(String key, Integer... defaults) {
            super(key, defaults);
        }

        @Override
        public Integer getValue() {
            return Integer.getInteger(getKey(), getDefault());
        }
    }

    private static abstract class LongJcstressProperty extends JcstressProperty<Long> {

        public LongJcstressProperty(String key, Long... defaults) {
            super(key, defaults);
        }

        @Override
        public Long getValue() {
            return Long.getLong(getKey(), getDefault());
        }
    }


    private static abstract class StringJcstressProperty extends JcstressProperty<String> {

        public StringJcstressProperty(String key, String... defaults) {
            super(key, defaults);
        }

        @Override
        public String getValue() {
            return System.getProperty(getKey(), getDefault());
        }
    }

    private static final IntJcstressProperty TIMEBUDGET_DEFAULTPERTESTMS = new IntJcstressProperty("jcstress.timeBudget.defaultPerTestMs", 3000) {
        @Override
        public String getDescription() {
            return getKey() + " set default time the individual test executes. Defaults to " + getDefault() +
                    "ms set to " + getValue() + "ms";
        }
    };

    private static final IntJcstressProperty TIMEBUDGET_MINTIMEMS = new IntJcstressProperty("jcstress.timeBudget.minTimeMs", 30) {
        @Override
        public String getDescription() {
            return getKey() + " set minimal time the individual test executes. Defaults to " + getDefault() +
                    "ms set to " + getValue() + "ms";
        }
    };

    private static final IntJcstressProperty TIMEBUDGET_MAXTIMEMS = new IntJcstressProperty("jcstress.timeBudget.maxTimeMs", 60_000) {
        @Override
        public String getDescription() {
            return getKey() +
                    " set maximum time the individual test executes. Defaults to " + getDefault() +
                    "ms set to " + getValue() + "ms";
        }
    };

    private static final String TIMEBUDGET_ADDITIONAL_COMMENT = "The time each test is run is (simplified) calculated as value of " + Options.TIME_BUDGET_SWITCH + " switch divided by number of tests (after all filters applied)" +
            " If te resulting time is smaller then " + TIMEBUDGET_MINTIMEMS.getKey() + ", it is used.  If it si bigger then " + TIMEBUDGET_MAXTIMEMS.getKey() + " it is used. If no " + Options.TIME_BUDGET_SWITCH + "  is set," +
            " then " + TIMEBUDGET_DEFAULTPERTESTMS.getKey() + " is used. See " + Options.TIME_BUDGET_SWITCH + " for more info. Properties do not accept unit suffixes.";

    private static final StringJcstressProperty LINK_ADDRESS = new StringJcstressProperty("jcstress.link.address", new String[]{null}) {
        @Override
        public String getDescription() {
            return getKey() + " is address where to connect to forked VMs. Defaults to loop-back. Set to '" + getListenAddressForInfo() + "'";
        }
    };


    private static final IntJcstressProperty LINK_PORT = new IntJcstressProperty("jcstress.link.port", 0) {
        @Override
        public String getDescription() {
            return getKey() + " is port where to connect to forked VMs on " + LINK_ADDRESS.getKey() + ". Defaults to " + getDefault() + " (random free port)." +
                    " Set to " + getJcstressLinkPort();
        }
    };

    private static final IntJcstressProperty LINK_TIMEOUTMS = new IntJcstressProperty("jcstress.link.timeoutMs", 30 * 1000) {
        @Override
        public String getDescription() {
            return getKey() + " set timeout to forked VM communication ms." +
                    " Defaults to " + getDefault() + "ms. Set to " + getValue() + "ms.";
        }
    };

    private static final LongJcstressProperty CONSOLE_PRINTINTERVALMS = new LongJcstressProperty("jcstress.console.printIntervalMs", 1_000L, 15_000L) {

        @Override
        public Long getValue() {
            return Long.getLong(getKey(), null);
        }

        @Override
        public String getDescription() {
            return getKey() + " sets interval how often to print results to console in ms. Have two defaults: " +
                    getDefaults().get(0) + "ms in interactive mode and " +
                    getDefaults().get(1) + "ms in noninteractive mode. Set to " +
                    getPrintIntervalMs() + "ms";
        }
    };

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

    private static String getListenAddressForInfo() {
        try {
            return getListenAddress().toString();
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    public static InetAddress getListenAddress() {
        // Try to use user-provided override first.
        if (getJcstressLinkAddress() != null) {
            try {
                return InetAddress.getByName(getJcstressLinkAddress());
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
        ouer.println("  " + TIMEBUDGET_DEFAULTPERTESTMS.getDescription());
        ouer.println("  " + TIMEBUDGET_MINTIMEMS.getDescription());
        ouer.println("  " + TIMEBUDGET_MAXTIMEMS.getDescription());
        ouer.println("  " + TIMEBUDGET_ADDITIONAL_COMMENT);
        ouer.println("  " + LINK_ADDRESS.getDescription());
        ouer.println("  " + LINK_PORT.getDescription());
        ouer.println("  " + LINK_TIMEOUTMS.getDescription());
        ouer.println("  " + CONSOLE_PRINTINTERVALMS.getDescription());
    }
}