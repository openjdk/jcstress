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
import org.openjdk.jcstress.util.Promise;
import org.openjdk.jcstress.util.StringUtils;
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
    private Promise<Integer> systemCPUs;
    private Promise<Integer> userCPUs;
    private int forks;
    private String mode;
    private boolean userYield;
    private String resultFile;
    private int deoptRatio;
    private Collection<String> jvmArgs;
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

        OptionSpec<Integer> cpus = parser.accepts("c", "Concurrency level for tests. This value can be greater " +
                "than number of CPUs available.")
                .withRequiredArg().ofType(Integer.class).describedAs("N");

        OptionSpec<Integer> sysCpus = parser.accepts("sc", "Number of CPUs in the system. Setting this value overrides " +
                "the autodetection.")
                .withRequiredArg().ofType(Integer.class).describedAs("N");

        OptionSpec<Integer> maxFootprint = parser.accepts("mf", "Maximum footprint for each test, in megabytes. This " +
                "affects the stride size: maximum footprint will never be exceeded, regardless of min/max stride sizes.")
                .withRequiredArg().ofType(Integer.class).describedAs("MB");

        OptionSpec<Integer> batchSize = parser.accepts("bs", "Maximum number of tests to execute in a single VM. Larger " +
                "values will improve test performance, at expense of testing accuracy")
                .withRequiredArg().ofType(Integer.class).describedAs("#");

        OptionSpec<Boolean> shouldYield = parser.accepts("yield", "Call Thread.yield() in busy loops.")
                .withOptionalArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<Integer> forks = parser.accepts("f", "Should fork each test N times. \"0\" to run in the embedded mode " +
                "with occasional forking.")
                .withOptionalArg().ofType(Integer.class).describedAs("count");

        OptionSpec<String> modeStr = parser.accepts("m", "Test mode preset: sanity, quick, default, tough, stress.")
                .withRequiredArg().ofType(String.class).describedAs("mode");

        OptionSpec<Integer> deoptRatio = parser.accepts("deoptRatio", "De-optimize (roughly) every N-th iteration. Larger " +
                "value improves test performance, but decreases the chance we hit unlucky compilation.")
                .withRequiredArg().ofType(Integer.class).describedAs("N");

        OptionSpec<String> optJvmArgs = parser.accepts("jvmArgs", "Use given JVM arguments. This disables JVM flags auto-detection, " +
                "and runs only the single JVM mode. Either a single space-separated option line, or multiple options are accepted. " +
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

        if (!set.hasArgument(sysCpus)) {
            this.systemCPUs = Promise.of(VMSupport::figureOutHotCPUs);
        } else {
            this.systemCPUs = Promise.of(set.valueOf(sysCpus));
        }

        if (!set.hasArgument(cpus)) {
            this.userCPUs = this.systemCPUs;
        } else {
            this.userCPUs = Promise.of(set.valueOf(cpus));
        }

        this.userYield = set.has(shouldYield);

        mode = orDefault(modeStr.value(set), "default");
        if (this.mode.equalsIgnoreCase("sanity")) {
            this.time = 50;
            this.iters = 1;
            this.forks = 0;
            this.batchSize = 20;
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
        this.deoptRatio = orDefault(set.valueOf(deoptRatio), this.iters * 3);

        if (set.hasArgument(optJvmArgs)) {
            try {
                List<String> vals = optJvmArgs.values(set);
                if (vals.size() != 1) {
                    jvmArgs = vals;
                } else {
                    jvmArgs = StringUtils.splitQuotedEscape(optJvmArgs.value(set));
                }
            } catch (OptionException e) {
                jvmArgs = StringUtils.splitQuotedEscape(optJvmArgs.value(set));
            }
        } else {
            jvmArgs = null;
        }

        return true;
    }

    private <T> T orDefault(T t, T def) {
        return (t != null) ? t : def;
    }

    public int getForks() {
        return forks;
    }

    public void printSettingsOn(PrintStream out) {
        out.printf("  Hardware threads in use/available: %d/%d, %s%n", getUserCPUs(), getSystemCPUs(), getSpinStyle());
        out.printf("  Test preset mode: \"%s\"\n", mode);
        out.printf("  Writing the test results to \"%s\"\n", resultFile);
        out.printf("  Parsing results to \"%s\"\n", resultDir);
        out.printf("  Running each test matching \"%s\" for %d forks, %d iterations, %d ms each\n", getTestFilter(), getForks(), getIterations(), getTime());
        out.printf("  Each JVM would execute at most %d tests in the row.\n", getBatchSize());
        out.printf("  Solo stride size will be autobalanced within [%d, %d] elements, but taking no more than %d Mb.\n", getMinStride(), getMaxStride(), getMaxFootprintMb());

        out.println();
    }

    public int deoptRatio() {
        return deoptRatio;
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
        if (VMSupport.spinWaitHintAvailable()) {
            return SpinLoopStyle.THREAD_SPIN_WAIT;
        } else {
            if (userYield) {
                return SpinLoopStyle.THREAD_YIELD;
            } else {
                return SpinLoopStyle.PLAIN;
            }
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

    public int getUserCPUs() {
        return userCPUs.get();
    }

    public int getSystemCPUs() {
        return systemCPUs.get();
    }

    public String getResultFile() {
        return resultFile;
    }

    public Collection<String> getJvmArgs() {
        return jvmArgs;
    }

    public int getMaxFootprintMb() {
        return maxFootprint;
    }

    public int getBatchSize() {
        return batchSize;
    }
}
