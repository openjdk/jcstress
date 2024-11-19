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

            output.println("<?xml version='1.0' encoding='UTF-8'?>");
            output.println("<testsuite name='jcstress'" +
                    " tests='" + totalCount + "'" +
                    " failures='" + failedCount + "'" +
                    " errors='" + 0/*fixme*/ + "'" +
                    " skipped='" + sanityFailedCount + "' " +
                    " time='" + 0/*fixme*/ + "'" +
                    " timestamp='" + new Date().toString() + ">");

        }
        {
            SortedMap<String, String> env = getEnv(byName);

            output.println("  <properties>");
            for (Map.Entry<String, String> entry : env.entrySet()) {
                output.println("    <property name='"+entry.getKey()+"' value='"+entry.getValue()+"' />");
            }
            output.println("    <property name='sparse' value='"+sparse+"' />");
            output.println("    <property name='"+useTestsuites+"' value='"+isTestsuiteUsed()+"' />");
            output.println("    <property name='"+errorAsFailure+"' value='"+isErrorAsFailure()+"' />");
            output.println("  </properties>");
        }

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

        printXTests(byName, output,
                "All tests",
                "",
                r -> true);
        if (sparse) {
            emitTestReports(ReportUtils.byName(collector.getTestResults()), output);
        } else {
            emitTestReports(ReportUtils.byDetailedName(collector.getTestResults()), output);
        }
        output.close();
    }

    private SortedMap<String, String> getEnv(List<TestResult> ts) {
        SortedMap<String, String> env = new TreeMap<>();
        for (TestResult result : ts) {
            if (result != null) {
                for (Map.Entry<String, String> kv : result.getEnv().entries().entrySet()) {
                    String key = kv.getKey();
                    String value = kv.getValue();
                    String lastV = env.get(key);
                    if (lastV == null) {
                        env.put(key, value);
                    } else {
                        // Some VMs have these keys pre-populated with the command line,
                        // which can have port definitions, PIDs, etc, and naturally
                        // clash from launch to launch.
                        if (key.equals("cmdLine")) continue;
                        if (key.equals("launcher")) continue;

                        if (!lastV.equalsIgnoreCase(value)) {
                            System.err.println("Mismatched environment for key = " + key + ", was = " + lastV + ", now = " + value);
                        }
                    }
                }
            }
        }
        return env;
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
            TestInfo test = TestList.getInfo(name.split(" ")[0]);
            local.println(resultDir + "/" + name + ".html would be...");
            emitTestReport(local, multiByName.get(name), test);
            local.close();
        });
    }

    public void emitTestReport(PrintWriter o, Collection<TestResult> results, TestInfo test) {
        o.println("subtests of: " + test.name());
        o.println("  Description and references:");
        o.println("  * " + test.description() + "");
        for (String ref : test.refs()) {
            o.println("    " + ref);
        }

        List<TestResult> sorted = new ArrayList<>(results);
        sorted.sort(Comparator
                .comparing((TestResult t) -> t.getConfig().getCompileMode())
                .thenComparing((TestResult t) -> t.getConfig().getSchedulingClass().toString())
                .thenComparing((TestResult t) -> StringUtils.join(t.getConfig().jvmArgs, ",")));


        o.println("<properties>");
        for (Map.Entry<String, String> entry : getEnv(sorted).entrySet()) {
            o.println("<property  name='" + entry.getKey() + "' value=" + entry.getValue() + "' />");
        }
        o.println("</properties>");

        Set<String> keys = new TreeSet<>();
        for (TestResult r : sorted) {
            keys.addAll(r.getStateKeys());
        }

// fixme use later? Defiitley ther emust be for (String key : keys) {for (TestResult r : sorted) {}} of results
//                GradingResult c = r.grading().gradingResults.get(key);
//                    o.println("<td>" + c.expect + "</td>");
//                    o.println("<td>" + c.description + "</td>");



        for (TestResult r : sorted) {
            String color = ReportUtils.statusToPassed(r) ? "green" : "red";
            String label = ReportUtils.statusToLabel(r);
            o.println(color + " " + label + " " + r.getConfig().toDetailedTest(false)); //TODO, keep using  the seed shading
// this is that multiplication. Probably just span it to failure if any?
//            for (String key : keys) {
//                GradingResult c = r.grading().gradingResults.get(key);
//                if (c != null) {
//                    o.println("<td align='right' width='" + 100D / keys.size() + "%' bgColor=" + selectHTMLColor(c.expect, c.count == 0) + ">" + c.count + "</td>");
//                } else {
//                    o.println("<td align='right' width='" + 100D / keys.size() + "%' bgColor=" + selectHTMLColor(Expect.ACCEPTABLE, true) + ">0</td>");
//                }
//            }
        }


        o.println("<h3>Messages</h3>");

        for (TestResult r : sorted) {
            if (!r.getMessages().isEmpty()) {
                resultHeader(o, r);
                o.println("<pre>");
                for (String data : r.getMessages()) {
                    o.println(data);
                }
                o.println("</pre>");
                o.println();
            }
        }

        o.println("<h3>VM Output Streams</h3>");

        for (TestResult r : sorted) {
            if (!r.getVmOut().isEmpty()) {
                resultHeader(o, r);
                o.println("<pre>");
                for (String data : r.getVmOut()) {
                    o.println(data);
                }
                o.println("</pre>");
                o.println();
            }
        }

        o.println("<h3>VM Error Streams</h3>");

        for (TestResult r : sorted) {
            if (!r.getVmErr().isEmpty()) {
                resultHeader(o, r);
                o.println("<pre>");
                for (String data : r.getVmErr()) {
                    o.println(data);
                }
                o.println("</pre>");
                o.println();
            }
        }

    }

    private void resultHeader(PrintWriter o, TestResult r) {
        TestConfig cfg = r.getConfig();
        o.println("<p><b>");
        o.println("<pre>" + CompileMode.description(cfg.compileMode, cfg.actorNames) + "</pre>");
        o.println("<pre>" + SchedulingClass.description(cfg.shClass, cfg.actorNames) + "</pre>");
        o.println("");
        if (!cfg.jvmArgs.isEmpty()) {
            o.println("<pre>" + cfg.jvmArgs + "</pre>");
        }
        o.println("</b></p>");
    }

    public Color selectColor(Expect type, boolean isZero) {
        switch (type) {
            case ACCEPTABLE:
                return isZero ? Color.LIGHT_GRAY : Color.GREEN;
            case FORBIDDEN:
                return isZero ? Color.LIGHT_GRAY : Color.RED;
            case ACCEPTABLE_INTERESTING:
                return isZero ? Color.LIGHT_GRAY : Color.CYAN;
            case UNKNOWN:
                return Color.RED;
            default:
                throw new IllegalStateException();
        }
    }

}
