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
package org.openjdk.jcstress.infra.grading;


import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.infra.Status;
import org.openjdk.jcstress.infra.TestInfo;
import org.openjdk.jcstress.infra.collectors.InProcessCollector;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.runners.TestConfig;
import org.openjdk.jcstress.infra.runners.TestList;
import org.openjdk.jcstress.os.SchedulingClass;
import org.openjdk.jcstress.util.Multimap;
import org.openjdk.jcstress.util.StringUtils;
import org.openjdk.jcstress.vm.CompileMode;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Prints HTML reports.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class XMLReportPrinter {

    public static final String ERROR_AS_FAILURE = "jcstress.xml.error2failure";
    public static final String USE_TESTSUITES = "jcstress.xml.testsuites";
    private final String resultDir;
    private final InProcessCollector collector;
    private final boolean sparse;
    private final boolean errorAsFailure;
    private final boolean useTestsuites;


    public static boolean isErrorAsFailure() {
        return "true".equals(System.getProperty(XMLReportPrinter.ERROR_AS_FAILURE));
    }

    public static boolean isTestsuiteUsed() {
        return "true".equals(System.getProperty(XMLReportPrinter.USE_TESTSUITES));
    }

    public XMLReportPrinter(String resultDir, InProcessCollector collector, PrintStream out, boolean sparse, boolean errorAsFailure, boolean useTestsuites) {
        //sparse true -ALL_MATCHING
        //sparse false - as ALL_MATCHING_COMBINATIONS
        //jednou smichat, jednou ne. Varovani kolik jich bude
        //-xml true/false, defaults to sparse
        this.collector = collector;
        this.resultDir = resultDir;
        this.sparse = sparse;
        this.errorAsFailure = errorAsFailure;
        this.useTestsuites = useTestsuites;
        File dir = new File(resultDir);
        dir.mkdirs();
        out.println("  " + getSparseString() + " XML report generated at " + dir.getAbsolutePath() + File.separator + getMainFileName() + ". " + ERROR_AS_FAILURE + "=" + errorAsFailure + ", " + USE_TESTSUITES + "=" + useTestsuites);
    }

    private String getMainFileName() {
        return "junit-" + getSparseString() + ".xml";
    }

    private String getSparseString() {
        return sparse ? "sparse" : "full";
    }

    public void work() throws FileNotFoundException {
        List<TestResult> byName = sparse ? ReportUtils.mergedByName(collector.getTestResults()) : new ArrayList<>(collector.getTestResults());
        Collections.sort(byName, Comparator.comparing(TestResult::getName));

        PrintWriter output = new PrintWriter(resultDir + File.separator + getMainFileName());

        {
            int passedCount = 0;
            int failedCount = 0;
            int sanityFailedCount = 0;
            for (TestResult result : byName) {
                if (result.status() == Status.NORMAL) {
                    if (result.grading().isPassed) {
                        passedCount++;
                    } else {
                        failedCount++;
                    }
                } else {
                    if (result.status() == Status.API_MISMATCH) {
                        sanityFailedCount++;
                    } else {
                        failedCount++;
                    }
                }
            }

            int totalCount = passedCount + failedCount + sanityFailedCount;

            String hostname="localhost";
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            }catch (Exception ex) {
                //no interest
            }
            output.println("<?xml version='1.0' encoding='UTF-8'?>");
            output.println("<testsuite name='jcstress'" +
                    " tests='" + totalCount + "'" +
                    " failures='" + failedCount + "'" +
                    " errors='" + 0/*fixme*/ + "'" +
                    " skipped='" + sanityFailedCount + "' " +
                    " time='" + 0/*fixme*/ + "'" +
                    " timestamp='" + new Date().toString() +
                    " hostname='" + hostname + ">");

        }
        {
            SortedMap<String, String> env = HTMLReportPrinter.getEnv(byName);

            output.println("  <properties>");
            for (Map.Entry<String, String> entry : env.entrySet()) {
                output.println("    <property name='"+entry.getKey()+"' value='"+entry.getValue()+"' />");
            }
            output.println("    <property name='sparse' value='"+sparse+"' />");
            output.println("    <property name='"+USE_TESTSUITES+"' value='"+isTestsuiteUsed()+"' />");
            output.println("    <property name='"+ERROR_AS_FAILURE+"' value='"+isErrorAsFailure()+"' />");
            output.println("  </properties>");
        }
// we have create dsummary, lets try to prnt the rest from merged info
        byName = ReportUtils.mergedByName(collector.getTestResults());
        Collections.sort(byName, Comparator.comparing(TestResult::getName));

        output.println("<!--");
        printXTests(byName, output,
                "FAILED tests",
                "Strong asserts were violated. Correct implementations should have no assert failures here.",
                r -> r.status() == Status.NORMAL && !r.grading().isPassed);

        printXTests(byName, output,
                "ERROR tests",
                "Tests break for some reason, other than failing the assert. Correct implementations should have none.",
                r -> r.status() != Status.NORMAL && r.status() != Status.API_MISMATCH);

        printXTests(byName, output,
                "INTERESTING tests",
                "Some interesting behaviors observed. This is for the plain curiosity.",
                r -> r.status() == Status.NORMAL && r.grading().hasInteresting);
        output.println("-->");

        emitTestReports(ReportUtils.byName(collector.getTestResults()), output);
        output.close();
    }

    private void printXTests(List<TestResult> byName,
                             PrintWriter output,
                             String header,
                             String subheader,
                             Predicate<TestResult> filterResults) {
        output.println("*** " + header + " ***");
        output.println("" + subheader + "");
        boolean hadAnyTests = false;
        for (TestResult result : byName) {
            if (filterResults.test(result)) {
                if (result.status() == Status.NORMAL) {
                    emitTest(output, result);
                } else {
                    emitTestFailure(output, result);
                }
                hadAnyTests = true;
            }
        }
        if (!hadAnyTests) {
            output.println("None!");
        }
    }

    public void emitTest(PrintWriter output, TestResult result) {
        TestGrading grading = result.grading();
        if (grading.isPassed) {
            output.println("  Passed - " + StringUtils.chunkName(result.getName()) + " " +getRoughCount(result));
        } else {
            output.println("  FAILED - " + StringUtils.chunkName(result.getName()) + " " +getRoughCount(result));
        }

        if (grading.hasInteresting) {
            output.println("    was interesting");
        }
    }

    public void emitTestFailure(PrintWriter output, TestResult result) {
        output.println("   FAILED - " + StringUtils.chunkName(result.getName()) + " " +getRoughCount(result));
        switch (result.status()) {
            case API_MISMATCH:
                output.println("      API MISMATCH - Sanity check failed, API mismatch?");
                break;
            case TEST_ERROR:
            case CHECK_TEST_ERROR:
                output.println("      ERROR - Error while running the test");
                break;
            case TIMEOUT_ERROR:
                output.println("      ERROR - Timeout while running the test");
                break;
            case VM_ERROR:
                output.println("      VM ERROR - Error running the VM");
                break;
        }
    }

    public static String getRoughCount(TestResult r) {
        long sum = r.getTotalCount();
        if (sum > 10) {
            return "10^" + (int) Math.floor(Math.log10(sum));
        } else {
            return String.valueOf(sum);
        }
    }

    private void emitTestReports(Multimap<String, TestResult> multiByName, PrintWriter local) {
        multiByName.keys().stream().forEach(name -> {
            TestInfo test = TestList.getInfo(name);
            local.println(resultDir + "/" + name + ".html would be...");
            emitTestReport(local, multiByName.get(name), test);
            local.close();
        });
    }

    public void emitTestReport(PrintWriter o, Collection<TestResult> results, TestInfo test) {
        //in sparse mode we print only test.name as test, with result based on cumulative
        //otherwise we weill be printing only its individual combinations (to mach the summary)
        if (sparse) {
            List<TestResult> sorted = new ArrayList<>(results);
            HTMLReportPrinter.resultsOrder(sorted);

            o.println("  <testcase class='jcstress' name='"+test.name());
            o.println("      <properties>");
            o.println("          <property name='description' value='"+test.description()+"'>");
            for (String ref : test.refs()) {
                o.println("          <property name='bug' value='"+ref+"'>");
            }
            for (Map.Entry<String, String> entry : HTMLReportPrinter.getEnv(sorted).entrySet()) {
                o.println("          <property name='"+entry.getKey()+"' value='"+entry.getValue()+"'>");
            }
            o.println("      </properties>");


            Set<String> keys = new TreeSet<>();
            for (TestResult r : sorted) {
                keys.addAll(r.getStateKeys());
            }
            for (TestResult r : sorted) {
                o.println("<failure>");
                o.println(r.getConfig().toDetailedTest(false));
                String color = ReportUtils.statusToPassed(r) ? "green" : "red";
                String label = ReportUtils.statusToLabel(r);
                o.println(color + " - " + label);

                for (String key : keys) {
                    GradingResult c = r.grading().gradingResults.get(key);
                    if (c != null) {
                        o.println(selectColor(c.expect, c.count == 0) + "/" + c.count + "");
                    } else {
                        o.println(selectColor(Expect.ACCEPTABLE, true) + "/0");
                    }
                }
                o.println("</failure>");
            }

            o.println("<system-out>");
            for (TestResult r : sorted) {
                if (!r.getMessages().isEmpty()) {
                    resultHeader(o, r);
                    for (String data : r.getMessages()) {
                        o.println(data);
                    }
                    o.println();
                }
                if (!r.getVmOut().isEmpty()) {
                    resultHeader(o, r);
                    for (String data : r.getVmOut()) {
                        o.println(data);
                    }
                    o.println();
                }
            }
            o.println("</system-out>");
            o.println("<system-err>");
            for (TestResult r : sorted) {
                if (!r.getVmErr().isEmpty()) {
                    resultHeader(o, r);                TestConfig cfg = r.getConfig();
                    for (String data : r.getVmErr()) {
                        o.println(data);
                    }
                    o.println();
                }
            }
            o.println("</system-err>\n");

            o.println("</testcase>");
        } else {
            List<TestResult> sorted = new ArrayList<>(results);
            HTMLReportPrinter.resultsOrder(sorted);
            for (TestResult r : sorted) {
                o.println("  <testcase class='jcstress' name='" + r.getConfig().toDetailedTest(false));
                o.println("      <properties>");
                o.println("          <property name='description' value='" + test.description() + "'>");
                for (String ref : test.refs()) {
                    o.println("          <property name='bug' value='" + ref + "'>");
                }
                for (Map.Entry<String, String> entry : HTMLReportPrinter.getEnv(sorted).entrySet()) {
                    o.println("          <property name='" + entry.getKey() + "' value='" + entry.getValue() + "'>");
                }
                o.println("      </properties>");

                Set<String> keys = new TreeSet<>();
                keys.addAll(r.getStateKeys());
                o.println("<failure>");

                TestConfig cfg = r.getConfig();
                o.println(r.getConfig().toDetailedTest(false));
                String color = ReportUtils.statusToPassed(r) ? "green" : "red";
                String label = ReportUtils.statusToLabel(r);
                o.println(color + " - " + label);

                for (String key : keys) {
                    GradingResult c = r.grading().gradingResults.get(key);
                    if (c != null) {
                        o.println(selectColor(c.expect, c.count == 0) + "/" + c.count + "");
                    } else {
                        o.println(selectColor(Expect.ACCEPTABLE, true) + "/0");
                    }
                }
                o.println("</failure>");
                o.println("<system-out>");
                if (!r.getMessages().isEmpty()) {
                    resultHeader(o, r);
                    for (String data : r.getMessages()) {
                        o.println(data);
                    }
                    o.println();
                }
                if (!r.getVmOut().isEmpty()) {
                    resultHeader(o, r);
                    for (String data : r.getVmOut()) {
                        o.println(data);
                    }
                    o.println();
                }
                o.println("</system-out>");
                o.println("<system-err>");
                if (!r.getVmErr().isEmpty()) {
                    resultHeader(o, r);
                    for (String data : r.getVmErr()) {
                        o.println(data);
                    }
                    o.println();
                }
                o.println("</system-err>\n");

                o.println("</testcase>");
            }
        }

    }

    private void resultHeader(PrintWriter o, TestResult r) {
        TestConfig cfg = r.getConfig();
        o.println("CompileMode: " + CompileMode.description(cfg.compileMode, cfg.actorNames));
        o.println("SchedulingClass" + SchedulingClass.description(cfg.shClass, cfg.actorNames));
        o.println("");
        if (!cfg.jvmArgs.isEmpty()) {
            o.println("jvmargs:" + cfg.jvmArgs);
        }
    }

    public String selectColor(Expect type, boolean isZero) {
        switch (type) {
            case ACCEPTABLE:
                return isZero ? "LIGHT_GRAY" : "GREEN";
            case FORBIDDEN:
                return isZero ? "LIGHT_GRAY" : "RED";
            case ACCEPTABLE_INTERESTING:
                return isZero ? "LIGHT_GRAY" : "CYAN";
            case UNKNOWN:
                return "RED";
            default:
                throw new IllegalStateException();
        }
    }

}
