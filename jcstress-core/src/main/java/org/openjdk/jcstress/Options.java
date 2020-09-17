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
import org.openjdk.jcstress.util.OptionFormatter;
import org.openjdk.jcstress.util.StringUtils;
import org.openjdk.jcstress.vm.DeoptMode;
import org.openjdk.jcstress.vm.VMSupport;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Options.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class Options {
    private String resultDir;
    private String testFilter;
    private int minStride, maxStride;
    private int maxFootprint;
    private int time;
    private int iters;
    private final String[] args;
    private boolean parse;
    private boolean list;
    private boolean verbose;
    private int totalCpuCount;
    private int cpuCount;
    private int forks;
    private String mode;
    private SpinLoopStyle spinStyle;
    private String resultFile;
    private DeoptMode deoptMode;
    private Collection<String> jvmArgs;
    private Collection<String> jvmArgsPrepend;
    private int batchSize;

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

        OptionSpec<Integer> maxFootprint = parser.accepts("mf", "Maximum footprint for each test, in megabytes. This " +
                "affects the stride size: maximum footprint will never be exceeded, regardless of min/max stride sizes.")
                .withRequiredArg().ofType(Integer.class).describedAs("MB");

        OptionSpec<Integer> batchSize = parser.accepts("bs", "Maximum number of tests to execute in a single VM. Larger " +
                "values will improve test performance, at expense of testing accuracy")
                .withRequiredArg().ofType(Integer.class).describedAs("#");

        OptionSpec<SpinLoopStyle> spinStyle = parser.accepts("spinStyle", "Busy loop wait style. " +
                "HARD = hard busy loop; THREAD_YIELD = use Thread.yield(); THREAD_SPIN_WAIT = use Thread.onSpinWait(); LOCKSUPPORT_PARK_NANOS = use LockSupport.parkNanos().")
                .withRequiredArg().ofType(SpinLoopStyle.class).describedAs("style");

        OptionSpec<Integer> forks = parser.accepts("f", "Should fork each test N times. \"0\" to run in the embedded mode " +
                "with occasional forking.")
                .withOptionalArg().ofType(Integer.class).describedAs("count");

        OptionSpec<String> modeStr = parser.accepts("m", "Test mode preset: sanity, quick, default, tough, stress.")
                .withRequiredArg().ofType(String.class).describedAs("mode");

        OptionSpec<DeoptMode> deoptMode = parser.accepts("deoptMode", "De-optimization mode, happens before each test. " +
                "NONE = No deoptimization. METHOD = Deoptimize org.openjdk.jcstress.*. ALL = Deoptimize everything.")
                .withRequiredArg().ofType(DeoptMode.class).describedAs("mode");

        OptionSpec<String> optJvmArgs = parser.accepts("jvmArgs", "Use given JVM arguments. This disables JVM flags auto-detection, " +
                "and runs only the single JVM mode. Either a single space-separated option line, or multiple options are accepted. " +
                "This option only affects forked runs.")
                .withRequiredArg().ofType(String.class).describedAs("string");

        OptionSpec<String> optJvmArgsPrepend = parser.accepts("jvmArgsPrepend", "Prepend given JVM arguments to auto-detected configurations. " +
                "Either a single space-separated option line, or multiple options are accepted. " +
                "This option only affects forked runs.")
                .withRequiredArg().ofType(String.class).describedAs("string");

        parser.accepts("v", "Be extra verbose.");
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

        this.minStride = orDefault(set.valueOf(minStride), 10);
        this.maxStride = orDefault(set.valueOf(maxStride), 10000);
        this.maxFootprint = orDefault(set.valueOf(maxFootprint), 100);
        this.testFilter = orDefault(set.valueOf(testFilter), ".*");

        this.parse = orDefault(set.has(parse), false);
        if (this.parse) {
            this.resultFile = set.valueOf(parse);
        } else {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.ROOT).format(new Date());
            this.resultFile = "jcstress-results-" + timestamp + ".bin.gz";
        }
        this.list = orDefault(set.has(list), false);
        this.verbose = orDefault(set.has("v"), false);

        totalCpuCount = VMSupport.figureOutHotCPUs();
        cpuCount = orDefault(set.valueOf(cpus), totalCpuCount);

        if (cpuCount > totalCpuCount) {
            System.err.println("Requested to use " + cpuCount + " CPUs, but system has only " + totalCpuCount + " CPUs.");
            System.err.println();
            parser.printHelpOn(System.err);
            return false;
        }

        this.spinStyle = orDefault(set.valueOf(spinStyle), SpinLoopStyle.THREAD_SPIN_WAIT);

        mode = orDefault(modeStr.value(set), "default");
        if (this.mode.equalsIgnoreCase("sanity")) {
            this.time = 10;
            this.iters = 1;
            this.forks = 1;
            this.batchSize = 100;
            this.deoptMode = DeoptMode.NONE;
        } else
        if (this.mode.equalsIgnoreCase("quick")) {
            this.time = 200;
            this.iters = 5;
            this.forks = 1;
            this.batchSize = 20;
        } else
        if (this.mode.equalsIgnoreCase("default")) {
            this.time = 1000;
            this.iters = 5;
            this.forks = 1;
            this.batchSize = 5;
        } else
        if (this.mode.equalsIgnoreCase("tough")) {
            this.time = 1000;
            this.iters = 10;
            this.forks = 10;
            this.batchSize = 5;
        } else
        if (this.mode.equalsIgnoreCase("stress")) {
            this.time = 1000;
            this.iters = 50;
            this.forks = 10;
            this.batchSize = 1;
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
        this.batchSize = orDefault(set.valueOf(batchSize), this.batchSize);
        this.deoptMode = orDefault(set.valueOf(deoptMode), DeoptMode.ALL);

        this.jvmArgs = processArgs(optJvmArgs, set);
        this.jvmArgsPrepend = processArgs(optJvmArgsPrepend, set);

        return true;
    }

    private Collection<String> processArgs(OptionSpec<String> op, OptionSet set) {
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
            return null;
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
        out.printf("  Each JVM would execute at most %d tests in the row.%n", getBatchSize());
        out.printf("  Solo stride size will be autobalanced within [%d, %d] elements, but taking no more than %d Mb.%n", getMinStride(), getMaxStride(), getMaxFootprintMb());

        out.println();
    }

    public DeoptMode deoptMode() {
        return deoptMode;
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

    public boolean shouldFork() {
        return forks > 0;
    }

    public int getIterations() {
        return iters;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public int getCPUCount() {
        return cpuCount;
    }

    public int getTotalCPUCount() {
        return totalCpuCount;
    }

    public String getResultFile() {
        return resultFile;
    }

    public Collection<String> getJvmArgs() {
        return jvmArgs;
    }

    public Collection<String> getJvmArgsPrepend() {
        return jvmArgsPrepend;
    }

    public int getMaxFootprintMb() {
        return maxFootprint;
    }

    public int getBatchSize() {
        return batchSize;
    }
}
