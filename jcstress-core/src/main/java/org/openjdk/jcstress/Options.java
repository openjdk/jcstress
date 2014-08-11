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
import org.openjdk.jcstress.util.OptionFormatter;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    private boolean shouldYield;
    private boolean parse;
    private boolean list;
    private boolean verbose;
    private String appendJvmArgs;
    private int systemCPUs;
    private int userCPUs;
    private int forks;
    private String mode;
    private String hostName;
    private Integer hostPort;
    private boolean forceYield;
    private boolean userYield;
    private String resultFile;
    private int deoptRatio;

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

        OptionSpec<Boolean> shouldYield = parser.accepts("yield", "Call Thread.yield() in busy loops.")
                .withOptionalArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<Integer> forks = parser.accepts("f", "Should fork each test N times. \"0\" to run in the embedded mode " +
                "with occasional forking, \"-1\" to never ever fork.")
                .withOptionalArg().ofType(Integer.class).describedAs("count");

        OptionSpec<String> appendJvmArgs = parser.accepts("jvmArgs", "Append these JVM arguments for the forked runs.")
                .withRequiredArg().ofType(String.class).describedAs("opts");

        OptionSpec<String> modeStr = parser.accepts("m", "Test mode preset: sanity, quick, default, tough, stress.")
                .withRequiredArg().ofType(String.class).describedAs("mode");

        OptionSpec<String> hostName = parser.accepts("hostName", "(internal) Host VM address")
                .withRequiredArg().ofType(String.class);

        OptionSpec<Integer> hostPort = parser.accepts("hostPort", "(internal) Host VM port")
                .withRequiredArg().ofType(Integer.class);

        OptionSpec<Integer> deoptRatio = parser.accepts("deoptRatio", "De-optimize (roughly) every N-th iteration. Larger " +
                "value improves test performance, but decreases the chance we hit unlucky compilation.")
                .withRequiredArg().ofType(Integer.class).describedAs("N");

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
        this.time = orDefault(set.valueOf(time), 1000);
        this.iters = orDefault(set.valueOf(iters), 5);
        this.testFilter = orDefault(set.valueOf(testFilter), ".*");
        this.deoptRatio = orDefault(set.valueOf(deoptRatio), 5);

        this.forks = orDefault(set.valueOf(forks), 1);
        this.parse = orDefault(set.has(parse), false);
        if (this.parse) {
            this.resultFile = set.valueOf(parse);
        } else {
            this.resultFile = "jcstress." + System.currentTimeMillis();
        }
        this.list = orDefault(set.has(list), false);
        this.appendJvmArgs = orDefault(set.valueOf(appendJvmArgs), "");
        this.verbose = orDefault(set.has("v"), false);

        this.hostName = set.valueOf(hostName);
        this.hostPort = set.valueOf(hostPort);

        if (!set.hasArgument(sysCpus)) {
            this.systemCPUs = figureOutHotCPUs();
        } else {
            this.systemCPUs = set.valueOf(sysCpus);
        }

        if (!set.hasArgument(cpus)) {
            this.userCPUs = this.systemCPUs;
        } else {
            this.userCPUs = set.valueOf(cpus);
        }

        if (userCPUs > systemCPUs) {
            forceYield = true;
        }

        this.userYield = set.has(shouldYield);
        this.shouldYield = orDefault(set.valueOf(shouldYield), forceYield);

        mode = orDefault(modeStr.value(set), "default");
        if (this.mode.equalsIgnoreCase("sanity")) {
            this.time = 50;
            this.iters = 1;
            this.forks = 0;
        } else
        if (this.mode.equalsIgnoreCase("quick")) {
            this.time = 300;
            this.iters = 5;
            this.forks = 0;
        } else
        if (this.mode.equalsIgnoreCase("default")) {
            // do nothing
        } else
        if (this.mode.equalsIgnoreCase("tough")) {
            this.time = 5000;
            this.iters = 10;
            this.forks = 10;
        } else
        if (this.mode.equalsIgnoreCase("stress")) {
            this.time = 1000;
            this.iters = 5;
            this.forks = 100;
        } else {
            System.err.println("Unknown test mode: " + this.mode);
            System.err.println();
            parser.printHelpOn(System.err);
            return false;
        }

        return true;
    }

    private <T> T orDefault(T t, T def) {
        return (t != null) ? t : def;
    }

    /**
     * Warm up the CPU schedulers, bring all the CPUs online to get the
     * reasonable estimate of the system capacity.
     *
     * @return online CPU count
     */
    private int figureOutHotCPUs() {
        ExecutorService service = Executors.newCachedThreadPool();

        System.out.print("Burning up to figure out the exact CPU count...");

        int warmupTime = 1000;
        long lastChange = System.currentTimeMillis();

        List<Future<?>> futures = new ArrayList<>();
        futures.add(service.submit(new BurningTask()));

        System.out.print(".");

        int max = 0;
        while (System.currentTimeMillis() - lastChange < warmupTime) {
            int cur = Runtime.getRuntime().availableProcessors();
            if (cur > max) {
                System.out.print(".");
                max = cur;
                lastChange = System.currentTimeMillis();
                futures.add(service.submit(new BurningTask()));
            }
        }

        for (Future<?> f : futures) {
            System.out.print(".");
            f.cancel(true);
        }

        service.shutdown();

        System.out.println(" done!");
        System.out.println();

        return max;
    }

    public int getForks() {
        return forks;
    }

    public void printSettingsOn(PrintStream out) {
        if (forks > 0) {
            out.println("FORKED MODE");
        } else {
            out.println("EMBEDDED MODE");
        }
        out.printf("  Test preset mode: \"%s\"\n", mode);
        out.printf("  Writing the test results to \"%s\"\n", resultFile);
        out.printf("  Parsing results to \"%s\"\n", resultDir);
        out.printf("  Running each test matching \"%s\" for %d forks, %d iterations, %d ms each\n", getTestFilter(), getForks(), getIterations(), getTime());
        out.printf("  Solo stride size will be autobalanced within [%d, %d] elements\n", getMinStride(), getMaxStride());
        out.printf("  Hardware threads in use/available: %d/%d, ", getUserCPUs(), getSystemCPUs());
        if (userYield) {
            if (shouldYield) {
                out.printf("user requested yielding in busy loops.\n");
            } else {
                out.printf("user disabled yielding in busy loops.\n");
            }
        } else {
            if (shouldYield) {
                out.printf("yielding was forced, more threads are requested than available.\n");
            } else {
                out.printf("no yielding in use.\n");
            }
        }

        out.println();
    }

    public int deoptRatio() {
        return deoptRatio;
    }

    public static class BurningTask implements Runnable {

        @Override
        public void run() {
            while (!Thread.interrupted()); // burn;
        }
    }

    public Collection<String> buildForkedCmdLine() {
        // omit -f, -p, -t
        Collection<String> cmdLine = new ArrayList<>();
        cmdLine.add("-r");
        cmdLine.add(resultDir);
        cmdLine.add("-minStride");
        cmdLine.add(Integer.toString(minStride));
        cmdLine.add("-maxStride");
        cmdLine.add(Integer.toString(maxStride));
        cmdLine.add("-time");
        cmdLine.add(Integer.toString(time));
        cmdLine.add("-iters");
        cmdLine.add(Integer.toString(iters));
        cmdLine.add("-yield");
        cmdLine.add(Boolean.toString(shouldYield));
        cmdLine.add("-c");
        cmdLine.add(Integer.toString(userCPUs));
        cmdLine.add("-sc");
        cmdLine.add(Integer.toString(systemCPUs));
        cmdLine.add("-f");
        cmdLine.add("0");
        if (verbose) cmdLine.add("-v");

        return  cmdLine;
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

    public boolean shouldYield() {
        return shouldYield;
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

    public boolean shouldNeverFork() {
        return forks < 0;
    }

    public int getIterations() {
        return iters;
    }

    public String getAppendJvmArgs() {
        return appendJvmArgs;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public int getUserCPUs() {
        return userCPUs;
    }

    public int getSystemCPUs() {
        return systemCPUs;
    }

    public String getHostName() {
        return hostName;
    }

    public int getHostPort() {
        return hostPort;
    }

    public String getResultFile() {
        return resultFile;
    }
}
