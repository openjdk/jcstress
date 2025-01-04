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

/**
 * Known Properties affecting JCStress. Some of them publicly document themselves, some do not need to.
 */
public class UsedProperties {
    private static final String JCSTRESS_TIMEBUDGET_DEFAULTPERTESTMS = "jcstress.timeBudget.defaultPerTestMs";
    private static final int JCSTRESS_TIMEBUDGET_DEFAULTPERTESTMS_DEFAULT = 3000;
    private static final String JCSTRESS_TIMEBUDGET_DEFAULTPERTESTMS_COMMENT = JCSTRESS_TIMEBUDGET_DEFAULTPERTESTMS +
            " set default time the individual test executes. Defaults to " + JCSTRESS_TIMEBUDGET_DEFAULTPERTESTMS_DEFAULT +
            "ms set to " + getJcstressTimeBudgetDefaultPerTestMs() + "ms";

    private static final String JCSTRESS_TIMEBUDGET_MINTIMEMS = "jcstress.timeBudget.minTimeMs";
    private static final int JCSTRESS_TIMEBUDGET_MINTIMEMS_DEFAULT = 30;
    private static final String JCSTRESS_TIMEBUDGET_MINTIMEMS_COMMENT = JCSTRESS_TIMEBUDGET_MINTIMEMS +
            " set minimal time the individual test executes. Defaults to " + JCSTRESS_TIMEBUDGET_MINTIMEMS_DEFAULT +
            "ms set to " + getJcstressTimeBudgetMinTimeMs() + "ms";

    private static final String JCSTRESS_TIMEBUDGET_MAXTIMEMS = "jcstress.timeBudget.maxTimeMs";
    private static final int JCSTRESS_TIMEBUDGET_MAXTIMEMS_DEFAULT = 60_000;
    private static final String JCSTRESS_TIMEBUDGET_MAXTIMEMS_COMMENT = JCSTRESS_TIMEBUDGET_MAXTIMEMS +
            " set maximum time the individual test executes. Defaults to " + JCSTRESS_TIMEBUDGET_MAXTIMEMS_DEFAULT +
            "ms set to " + getJcstressTimeBudgetMaxTimeMs() + "ms";

    private static final String JCSTRESS_TIMEBUDGET_ADDITIONAL_COMMENT = "The time each test is run is (simplified) calculated as value of " + Options.TIME_BUDGET_SWITCH + " switch divided by number of tests (after all filters applied)" +
            " If te resulting time is smaller then " + JCSTRESS_TIMEBUDGET_MINTIMEMS + ", it is used.  If it si bigger then " + JCSTRESS_TIMEBUDGET_MAXTIMEMS + " it is used. If no " + Options.TIME_BUDGET_SWITCH + "  is set," +
            " then " + JCSTRESS_TIMEBUDGET_DEFAULTPERTESTMS + " is used. See " + Options.TIME_BUDGET_SWITCH + " for more info. Properties do not accept unit suffixes.";

    private static final String JCSTRESS_LINK_ADDRESS = "jcstress.link.address";
    private static final String JCSTRESS_LINK_ADDRESS_COMMENT = JCSTRESS_LINK_ADDRESS + " is address where to connect to forked VMs. Defaults to loop-back. Set to '" + getListenAddressForInfo() + "'";

    private static final String JCSTRESS_LINK_PORT = "jcstress.link.port";
    private static final int JCSTRESS_LINK_PORT_DEFAULT = 0;
    private static final String JCSTRESS_LINK_PORT_COMMENT = JCSTRESS_LINK_PORT + " is port where to connect to forked VMs on " + JCSTRESS_LINK_ADDRESS + ". Defaults to " + JCSTRESS_LINK_PORT_DEFAULT + " (random free port)." +
            " Set to " + getJcstressLinkPort();

    private static final String JCSTRESS_LINK_TIMEOUTMS = "jcstress.link.timeoutMs";
    private static final int JCSTRESS_LINK_TIMEOUTMS_DEFAULT = 30 * 1000;
    private static final String JCSTRESS_LINK_TIMEOUTMS_COMMENT = JCSTRESS_LINK_TIMEOUTMS + " set timeout to forked VM communication ms." +
            " Defaults to " + JCSTRESS_LINK_TIMEOUTMS_DEFAULT + "ms. Set to " + getJcstressLinkTimeoutMs() + "ms.";

    private static final String JCSTRESS_CONSOLE_PRINTINTERVALMS = "jcstress.console.printIntervalMs";
    private static final long JCSTRESS_CONSOLE_PRINTINTERVALMS_INTERACTIVE_DEFAULT = 1_000;
    private static final long JCSTRESS_CONSOLE_PRINTINTERVALMS_NONINTERACTIVE_DEFAULT = 15_000;
    private static final String JCSTRESS_CONSOLE_PRINTINTERVALMS_COMMENT = JCSTRESS_CONSOLE_PRINTINTERVALMS + " sets interval how often to print results to console in ms. Have two defaults: " +
            JCSTRESS_CONSOLE_PRINTINTERVALMS_INTERACTIVE_DEFAULT + "ms in interactive mode and " +
            JCSTRESS_CONSOLE_PRINTINTERVALMS_NONINTERACTIVE_DEFAULT + "ms in noninteractive mode. Set to " +
            getPrintIntervalMs() + "ms";

    public static int getJcstressTimeBudgetDefaultPerTestMs() {
        return Integer.getInteger(JCSTRESS_TIMEBUDGET_DEFAULTPERTESTMS, JCSTRESS_TIMEBUDGET_DEFAULTPERTESTMS_DEFAULT);
    }

    public static int getJcstressTimeBudgetMinTimeMs() {
        return Integer.getInteger(JCSTRESS_TIMEBUDGET_MINTIMEMS, JCSTRESS_TIMEBUDGET_MINTIMEMS_DEFAULT);
    }

    public static int getJcstressTimeBudgetMaxTimeMs() {
        return Integer.getInteger(JCSTRESS_TIMEBUDGET_MAXTIMEMS, JCSTRESS_TIMEBUDGET_MAXTIMEMS_DEFAULT);
    }

    private static String getJcstressLinkAddress() {
        return System.getProperty(JCSTRESS_LINK_ADDRESS);
    }

    public static String getListenAddressForInfo() {
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
        return Integer.getInteger(JCSTRESS_LINK_PORT, JCSTRESS_LINK_PORT_DEFAULT);
    }


    public static int getJcstressLinkTimeoutMs() {
        return Integer.getInteger(JCSTRESS_LINK_TIMEOUTMS, JCSTRESS_LINK_TIMEOUTMS_DEFAULT);
    }

    public static boolean isProgressInteractive() {
        return System.console() != null;
    }

    private static String getJcstressConsolePrintIntervalMs() {
        return System.getProperty(JCSTRESS_CONSOLE_PRINTINTERVALMS);
    }

    public static long getPrintIntervalMs() {
        return (getJcstressConsolePrintIntervalMs() != null) ?
                Long.parseLong(getJcstressConsolePrintIntervalMs()) :
                isProgressInteractive() ? JCSTRESS_CONSOLE_PRINTINTERVALMS_INTERACTIVE_DEFAULT : JCSTRESS_CONSOLE_PRINTINTERVALMS_NONINTERACTIVE_DEFAULT;
    }

    public static void printHelpOn(PrintStream ouer) {
        ouer.println("JCStress recognize several internal properties:");
        ouer.println("  " + JCSTRESS_TIMEBUDGET_DEFAULTPERTESTMS_COMMENT);
        ouer.println("  " + JCSTRESS_TIMEBUDGET_MINTIMEMS_COMMENT);
        ouer.println("  " + JCSTRESS_TIMEBUDGET_MAXTIMEMS_COMMENT);
        ouer.println("  " + JCSTRESS_TIMEBUDGET_ADDITIONAL_COMMENT);
        ouer.println("  " + JCSTRESS_LINK_ADDRESS_COMMENT);
        ouer.println("  " + JCSTRESS_LINK_PORT_COMMENT);
        ouer.println("  " + JCSTRESS_LINK_TIMEOUTMS_COMMENT);
        ouer.println("  " + JCSTRESS_CONSOLE_PRINTINTERVALMS_COMMENT);


    }
}