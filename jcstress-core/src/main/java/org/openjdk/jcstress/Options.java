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

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.openjdk.jcstress.infra.runners.SpinLoopStyle;
import org.openjdk.jcstress.os.AffinityMode;
import org.openjdk.jcstress.util.OptionFormatter;
import org.openjdk.jcstress.util.StringUtils;
import org.openjdk.jcstress.vm.VMSupport;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Options.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class Options {
    private String resultDir;
    private String testFilter;
    private int minStride, maxStride;
    private int time;
    private int iters;
    private final String[] args;
    private boolean parse;
    private boolean list;
    private Verbosity verbosity;
    private int cpuCount;
    private int heapPerFork;
    private int forks;
    private String mode;
    private SpinLoopStyle spinStyle;
    private String resultFile;
    private List<String> jvmArgs;
    private List<String> jvmArgsPrepend;
    private boolean splitCompilation;
    private AffinityMode affinityMode;

    public Options(String[] args) {
        this.args = args;
    }

    public boolean parse() throws IOException {
        OptionParser parser = new OptionParser();
        parser.formatHelpWith(new OptionFormatter());

        OptionSpec<String> result = parser.accepts("r", "Target destination to put the report into.")
                .withRequiredArg().ofType(String.class).describedAs("dir");

        OptionSpec<String> parse = parser.accepts("p", "Re-run parser on the result file. This will not run any tests.")
                .withRequiredArg().ofType(String.class).describedAs("result file");

        OptionSpec<Boolean> list = parser.accepts("l", "List the available tests matching the requested settings.")
                .withOptionalArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<String> testFilter = parser.accepts("t", "Regexp selector for tests.")
                .withRequiredArg().ofType(String.class).describedAs("regexp");

        OptionSpec<Integer> minStride = parser.accepts("minStride", "Minimum internal stride size. Larger value decreases " +
                "the synchronization overhead, but also reduces accuracy.")
                .withRequiredArg().ofType(Integer.class).describedAs("N");

        OptionSpec<Integer> maxStride = parser.accepts("maxStride", "Maximum internal stride size. Larger value decreases " +
                "the synchronization overhead, but also reduces accuracy.")
                .withRequiredArg().ofType(Integer.class).describedAs("N");

        OptionSpec<Integer> time = parser.accepts("time", "Time to spend in single test iteration. Larger value improves " +
                "test reliability, since schedulers do better job in the long run.")
                .withRequiredArg().ofType(Integer.class).describedAs("ms");

        OptionSpec<Integer> iters = parser.accepts("iters", "Iterations per test.")
                .withRequiredArg().ofType(Integer.class).describedAs("N");

        OptionSpec<Integer> cpus = parser.accepts("c", "Number of CPUs to use. Defaults to all CPUs in the system. " +
                "Reducing the number of CPUs limits the amount of resources (including memory) the run is using.")
                .withRequiredArg().ofType(Integer.class).describedAs("N");

        OptionSpec<Integer> heapPerFork = parser.accepts("hs", "Java heap size per fork, in megabytes. This " +
                "affects the stride size: maximum footprint will never be exceeded, regardless of min/max stride sizes.")
                .withRequiredArg().ofType(Integer.class).describedAs("MB");

        OptionSpec<SpinLoopStyle> spinStyle = parser.accepts("spinStyle", "Busy loop wait style. " +
                "HARD = hard busy loop; THREAD_YIELD = use Thread.yield(); THREAD_SPIN_WAIT = use Thread.onSpinWait(); LOCKSUPPORT_PARK_NANOS = use LockSupport.parkNanos().")
                .withRequiredArg().ofType(SpinLoopStyle.class).describedAs("style");

        OptionSpec<Integer> forks = parser.accepts("f", "Should fork each test N times. \"0\" to run in the embedded mode " +
                "with occasional forking.")
                .withOptionalArg().ofType(Integer.class).describedAs("count");

        OptionSpec<String> modeStr = parser.accepts("m", "Test mode preset: sanity, quick, default, tough, stress.")
                .withRequiredArg().ofType(String.class).describedAs("mode");

        OptionSpec<String> optJvmArgs = parser.accepts("jvmArgs", "Use given JVM arguments. This disables JVM flags auto-detection, " +
                "and runs only the single JVM mode. Either a single space-separated option line, or multiple options are accepted. " +
                "This option only affects forked runs.")
                .withRequiredArg().ofType(String.class).describedAs("string");

        OptionSpec<String> optJvmArgsPrepend = parser.accepts("jvmArgsPrepend", "Prepend given JVM arguments to auto-detected configurations. " +
                "Either a single space-separated option line, or multiple options are accepted. " +
                "This option only affects forked runs.")
                .withRequiredArg().ofType(String.class).describedAs("string");

        OptionSpec<Boolean> optSplitCompilation = parser.accepts("sc", "Use split per-actor compilation mode, if available.")
                .withOptionalArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<AffinityMode> optAffinityMode = parser.accepts("af", "Use the specific affinity mode, if available.")
                .withOptionalArg().ofType(AffinityMode.class).describedAs("mode");

        parser.accepts("v", "Be verbose.");
        parser.accepts("vv", "Be extra verbose.");
        parser.accepts("vvv", "Be extra extra verbose.");
        parser.accepts("h", "Print this help.");

        OptionSet set;
        try {
            set = parser.parse(args);
        } catch (OptionException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.err.println();
            parser.printHelpOn(System.err);
            return false;
        }

        if (set.has("h")) {
            parser.printHelpOn(System.out);
            return false;
        }

        this.resultDir = orDefault(set.valueOf(result), "results/");
        if (!resultDir.endsWith("/")) {
            resultDir += "/";
        }

        this.testFilter = orDefault(set.valueOf(testFilter), ".*");

        this.parse = orDefault(set.has(parse), false);
        if (this.parse) {
            this.resultFile = set.valueOf(parse);
        } else {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.ROOT).format(new Date());
            this.resultFile = "jcstress-results-" + timestamp + ".bin.gz";
        }
        this.list = orDefault(set.has(list), false);
        if (set.has("vvv")) {
            this.verbosity = new Verbosity(3);
        } else if (set.has("vv")) {
            this.verbosity = new Verbosity(2);
        } else if (set.has("v")) {
            this.verbosity = new Verbosity(1);
        } else {
            this.verbosity = new Verbosity(0);
        }

        int totalCpuCount = VMSupport.figureOutHotCPUs();
        cpuCount = orDefault(set.valueOf(cpus), totalCpuCount);

        if (cpuCount > totalCpuCount) {
            System.err.println("Requested to use " + cpuCount + " CPUs, but system has only " + totalCpuCount + " CPUs.");
            System.err.println();
            parser.printHelpOn(System.err);
            return false;
        }

        this.spinStyle = orDefault(set.valueOf(spinStyle), SpinLoopStyle.THREAD_SPIN_WAIT);

        this.time = 1000;
        this.iters = 5;
        this.forks = 1;
        this.minStride = 10;
        this.maxStride = 10000;

        mode = orDefault(modeStr.value(set), "default");
        if (this.mode.equalsIgnoreCase("sanity")) {
            this.time = 0;
            this.iters = 1;
            this.forks = 1;
            this.minStride = 1;
            this.maxStride = 1;
        } else
        if (this.mode.equalsIgnoreCase("quick")) {
            this.time = 200;
        } else
        if (this.mode.equalsIgnoreCase("default")) {
            // Nothing changed.
        } else
        if (this.mode.equalsIgnoreCase("tough")) {
            this.iters = 10;
            this.forks = 10;
        } else
        if (this.mode.equalsIgnoreCase("stress")) {
            this.iters = 50;
            this.forks = 10;
        } else {
            System.err.println("Unknown test mode: " + this.mode);
            System.err.println();
            parser.printHelpOn(System.err);
            return false;
        }

        // override these, if present
        this.time = orDefault(set.valueOf(time), this.time);
        this.iters = orDefault(set.valueOf(iters), this.iters);
        this.forks = orDefault(set.valueOf(forks), this.forks);
        this.minStride = orDefault(set.valueOf(minStride), this.minStride);
        this.maxStride = orDefault(set.valueOf(maxStride), this.maxStride);

        this.heapPerFork = orDefault(set.valueOf(heapPerFork), 256);

        this.jvmArgs = processArgs(optJvmArgs, set);
        this.jvmArgsPrepend = processArgs(optJvmArgsPrepend, set);

        this.splitCompilation = orDefault(set.valueOf(optSplitCompilation), true);
        this.affinityMode = orDefault(set.valueOf(optAffinityMode), AffinityMode.LOCAL);

        return true;
    }

    private List<String> processArgs(OptionSpec<String> op, OptionSet set) {
        if (set.hasArgument(op)) {
            try {
                List<String> vals = op.values(set);
                if (vals.size() != 1) {
                    return vals;
                } else {
                    return StringUtils.splitQuotedEscape(op.value(set));
                }
            } catch (OptionException e) {
                return StringUtils.splitQuotedEscape(op.value(set));
            }
        } else {
            return Collections.emptyList();
        }
    }

    private <T> T orDefault(T t, T def) {
        return (t != null) ? t : def;
    }

    public int getForks() {
        return forks;
    }

    public void printSettingsOn(PrintStream out) {
        out.printf("  Hardware CPUs in use: %d, %s%n", getCPUCount(), getSpinStyle());
        out.printf("  Test preset mode: \"%s\"%n", mode);
        out.printf("  Writing the test results to \"%s\"%n", resultFile);
        out.printf("  Parsing results to \"%s\"%n", resultDir);
        out.printf("  Running each test matching \"%s\" for %d forks, %d iterations, %d ms each%n", getTestFilter(), getForks(), getIterations(), getTime());
        out.printf("  Solo stride size will be autobalanced within [%d, %d] elements, but taking no more than %d Mb.%n", getMinStride(), getMaxStride(), getMaxFootprintMb());

        out.println();
    }

    public int getMinStride() {
        return minStride;
    }

    public int getMaxStride() {
        return maxStride;
    }

    public String getResultDest() {
        return resultDir;
    }

    public int getTime() {
        return time;
    }

    public SpinLoopStyle getSpinStyle() {
        switch (spinStyle) {
            case HARD:
            case THREAD_YIELD:
            case LOCKSUPPORT_PARK_NANOS:
                return spinStyle;
            case THREAD_SPIN_WAIT:
                if (VMSupport.spinWaitHintAvailable()) {
                    return spinStyle;
                } else {
                    return SpinLoopStyle.HARD;
                }
            default:
                throw new IllegalStateException("Unhandled spin style: " + spinStyle);
        }
    }

    public boolean shouldParse() {
        return parse;
    }

    public boolean shouldList() {
        return list;
    }

    public String getTestFilter() {
        return testFilter;
    }

    public int getIterations() {
        return iters;
    }

    public Verbosity verbosity() {
        return verbosity;
    }

    public int getCPUCount() {
        return cpuCount;
    }

    public String getResultFile() {
        return resultFile;
    }

    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    public List<String> getJvmArgsPrepend() {
        return jvmArgsPrepend;
    }

    public int getHeapPerForkMb() {
        return heapPerFork;
    }

    public int getMaxFootprintMb() {
        // Half of heap size.
        return getHeapPerForkMb() / 2;
    }

    public boolean isSplitCompilation() {
        return splitCompilation;
    }

    public AffinityMode affinityMode() {
        return affinityMode;
    }
}
