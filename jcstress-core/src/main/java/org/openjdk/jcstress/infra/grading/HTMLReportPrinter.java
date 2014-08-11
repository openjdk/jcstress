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


import org.openjdk.jcstress.Options;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.infra.State;
import org.openjdk.jcstress.infra.StateCase;
import org.openjdk.jcstress.infra.Status;
import org.openjdk.jcstress.infra.TestInfo;
import org.openjdk.jcstress.infra.collectors.InProcessCollector;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.runners.TestList;
import org.openjdk.jcstress.util.Environment;
import org.openjdk.jcstress.util.HashMultimap;
import org.openjdk.jcstress.util.LongHashMultiset;
import org.openjdk.jcstress.util.Multimap;
import org.openjdk.jcstress.util.TreeMultimap;

import javax.xml.bind.JAXBException;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Prints HTML reports.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class HTMLReportPrinter {

    private final String resultDir;
    private final InProcessCollector collector;
    private int cellStyle = 1;

    private final ConsoleReportPrinter printer;
    private final boolean verbose;

    public HTMLReportPrinter(Options opts, InProcessCollector collector) throws JAXBException, FileNotFoundException {
        this.collector = collector;
        this.printer = new ConsoleReportPrinter(opts, new PrintWriter(System.out, true), 0);
        this.resultDir = opts.getResultDest();
        this.verbose = opts.isVerbose();
        new File(resultDir).mkdirs();
    }

    public void parse() throws FileNotFoundException, JAXBException {

        Map<String, TestResult> results = new TreeMap<>();

        {
            Multimap<String, TestResult> multiResults = new HashMultimap<>();
            for (TestResult r : collector.getTestResults()) {
                multiResults.put(r.getName(), r);
            }

            for (String name : multiResults.keys()) {
                Collection<TestResult> mergeable = multiResults.get(name);

                LongHashMultiset<State> stateCounts = new LongHashMultiset<>();

                List<String> auxData = new ArrayList<>();

                Status status = Status.NORMAL;
                Environment env = null;
                for (TestResult r : mergeable) {
                    status = status.combine(r.status());
                    for (State s : r.getStates()) {
                        stateCounts.add(s, s.getCount());
                    }
                    env = r.getEnv();
                    auxData.addAll(r.getAuxData());
                }

                TestResult root = new TestResult(name, status);

                for (State s : stateCounts.keys()) {
                    root.addState(s.getKey(), stateCounts.count(s));
                }

                root.setEnv(env);

                for (String data : auxData) {
                    root.addAuxData(data);
                }

                results.put(name, root);
            }
        }

        // build prefixes
        Multimap<String, String> packages = new TreeMultimap<>();
        for (String k : results.keySet()) {
            String pack = k.substring(0, k.lastIndexOf("."));
            packages.put(pack, k);
        }

        PrintWriter output = new PrintWriter(resultDir + "/index.html");

        output.println("\n" +
                "<html>\n" +
                "<head>\n" +
                "<title>Java Concurrency Stress test report</title>\n" +
                " <style type=\"text/css\">\n" +
                "   table { font-size: 9pt; }\n" +
                "   a { color: #000000; }\n" +
                "   .progress { padding: 0px; }\n" +
                "   .header { text-align: left; }\n" +
                "   .section1 { font-size: 12pt; background-color: #BDB76B; color: #000000; font-weight: bold;}\n" +
                "   .section2 { font-size: 12pt; background-color: #F0E68C; color: #000000; font-weight: bold;}\n" +
                "   .cell1 { background-color: #FAFAD2; }\n" +
                "   .cell2 { background-color: #EEE8AA; }\n" +
                "   .passedProgress { background-color: #00AA00; color: #FFFFFF; text-align: center; font-weight: bold; }\n" +
                "   .failedProgress { background-color: #FF0000; color: #FFFFFF; text-align: center; font-weight: bold; }\n" +
                "   .passed { color: #00AA00; text-align: center; font-weight: bold; }\n" +
                "   .failed { color: #FF0000; text-align: center; font-weight: bold; }\n" +
                "   .interesting { color: #0000FF; text-align: center; font-weight: bold; }\n" +
                "   .spec { color: #AAAA00; text-align: center; font-weight: bold; }\n" +
                "   .endResult { font-size: 48pt; text-align: center; font-weight: bold; }\n" +
                " </style>\n" +
                "</head>\n" +
                "<body>");

        output.println("<table width=\"100%\" cellspacing=\"20\">");
        output.println("<tr>");
        output.println("<td>");

        {
            int passedCount = 0;
            int failedCount = 0;
            int sanityFailedCount = 0;
            for (String k : packages.keys()) {
                Collection<String> testNames = packages.get(k);
                for (String testName : testNames) {
                    TestInfo test = TestList.getInfo(testName);
                    TestResult result = results.get(testName);
                    if (result.status() == Status.NORMAL) {
                        if (new TestGrading(result, test).isPassed) {
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
            }

            int totalCount = passedCount + failedCount;
            int passedProgress = totalCount > 0 ? (passedCount * 100 / totalCount) : 0;
            int failedProgress = totalCount > 0 ? (failedCount * 100 / totalCount) : 100;

            if (failedCount > 0) {
                output.println("<p class=\"endResult failed\">");
            } else {
                output.println("<p class=\"endResult passed\">");
            }
            output.println("" + passedProgress + "%");
            if (sanityFailedCount > 0) {
                output.println(" <span class=\"special\">(" + sanityFailedCount + " tests skipped)</span>");
            }
            output.println("</p>");

            output.println("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>");
            output.println("<td nowrap><b>Overall pass rate:</b> " + passedCount + "/" + (passedCount + failedCount) + "&nbsp;</td>");
            if (passedProgress > 0) {
                output.println("<td width=\"" + passedProgress + "%\" class=\"passedProgress\">&nbsp;</td>");
            }
            if (failedProgress > 0) {
                output.println("<td width=\"" + failedProgress + "%\" class=\"failedProgress\">&nbsp;</td>");
            }
            output.println("</tr></table>");

            output.println("<br>");
        }
        output.println("</td>");
        output.println("<td width=100>");

        {
            SortedMap<String, String> env = new TreeMap<>();
            for (String k : packages.keys()) {
                for (String testName : packages.get(k)) {
                    TestResult result = results.get(testName);
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
                                    System.err.println("Mismatched environment for key = " + key + ", was = " + lastV + ", now = "  + value);
                                }
                            }
                        }
                    }
                }
            }

            output.println("<table>");
            for (Map.Entry<String, String> entry : env.entrySet()) {
                output.println("<tr>");
                output.println("<td nowrap>" + entry.getKey() + "</td>");
                output.println("<td>" + entry.getValue() + "</td>");
                output.println("</tr>");
            }
            output.println("</table>");
        }

        output.println("</td>");
        output.println("</tr>");
        output.println("</table>");

        printFailedTests(results, packages, output);
        printErrorTests(results, packages, output);
        printSpecTests(results, packages, output);
        printInterestingTests(results, packages, output);
        printAllTests(results, packages, output);

        output.println("</body>");
        output.println("</html>");

        output.close();

        if (verbose) {
            for (String k : results.keySet()) {
                TestResult result = results.get(k);
                printer.add(result);
            }
        }
    }

    private void printFailedTests(Map<String, TestResult> results, Multimap<String, String> packages, PrintWriter output) throws FileNotFoundException, JAXBException {
        output.println("<h3>FAILED tests:<br>");
        output.println("&nbsp;Some asserts have been violated.<br>&nbsp;Correct implementations should have none.</h3>");
        output.println("<table cellspacing=0 cellpadding=0 width=\"100%\">");

        boolean hadAnyTests = false;
        for (String k : packages.keys()) {
            Collection<String> testNames = packages.get(k);

            boolean packageEmitted = false;
            for (String testName : testNames) {
                TestInfo test = TestList.getInfo(testName);
                TestResult result = results.get(testName);
                TestGrading grading = new TestGrading(result, test);
                if (result.status() == Status.NORMAL && !grading.isPassed) {
                    if (!packageEmitted) {
                        emitPackage(output, k);
                        packageEmitted = true;
                    }
                    emitTest(output, result, test);
                    hadAnyTests = true;
                }
            }
        }

        output.println("</table>");
        if (!hadAnyTests) {
            output.println("None!");
            output.println("<br>");
        }

        output.println("<br>");
    }


    private void printErrorTests(Map<String, TestResult> results, Multimap<String, String> packages, PrintWriter output) throws FileNotFoundException, JAXBException {
        output.println("<h3>ERROR tests:<br>");
        output.println("&nbsp;Tests break for some reason, other than failing the assert.<br>&nbsp;Correct implementations should have none.</h3>");
        output.println("<table cellspacing=0 cellpadding=0 width=\"100%\">");

        boolean hadAnyTests = false;
        for (String k : packages.keys()) {
            Collection<String> testNames = packages.get(k);

            boolean packageEmitted = false;
            for (String testName : testNames) {
                TestInfo test = TestList.getInfo(testName);
                TestResult result = results.get(testName);
                if (result.status() != Status.NORMAL && result.status() != Status.API_MISMATCH) {
                    if (!packageEmitted) {
                        emitPackage(output, k);
                        packageEmitted = true;
                    }
                    emitTestFailure(output, result, test);
                    hadAnyTests = true;
                }
            }
        }

        output.println("</table>");
        if (!hadAnyTests) {
            output.println("None!");
            output.println("<br>");
        }

        output.println("<br>");
    }

    private void printInterestingTests(Map<String, TestResult> results, Multimap<String, String> packages, PrintWriter output) throws FileNotFoundException, JAXBException {
        output.println("<h3>INTERESTING tests:<br>");
        output.println("&nbsp;Some interesting behaviors observed.<br>&nbsp;This is for the plain curiosity.</h3>");
        output.println("<table cellspacing=0 cellpadding=0 width=\"100%\">");

        boolean hadAnyTests = false;
        for (String k : packages.keys()) {
            Collection<String> testNames = packages.get(k);

            boolean packageEmitted = false;
            for (String testName : testNames) {
                TestInfo test = TestList.getInfo(testName);
                TestResult result = results.get(testName);
                TestGrading grading = new TestGrading(result, test);
                if (grading.hasInteresting) {
                    if (!packageEmitted) {
                        emitPackage(output, k);
                        packageEmitted = true;
                    }
                    emitTest(output, result, test);
                    hadAnyTests = true;
                }
            }
        }

        output.println("</table>");
        if (!hadAnyTests) {
            output.println("None!");
            output.println("<br>");
        }
        output.println("<br>");
    }

    private void printSpecTests(Map<String, TestResult> results, Multimap<String, String> packages, PrintWriter output) throws FileNotFoundException, JAXBException {
        output.println("<h3>SPEC tests:<br>");
        output.println("&nbsp;Formally acceptable, but surprising results are observed.<br>&nbsp;Implementations going beyond the minimal requirements should have none.</h3>");
        output.println("<table cellspacing=0 cellpadding=0 width=\"100%\">");

        boolean hadAnyTests = false;
        for (String k : packages.keys()) {
            Collection<String> testNames = packages.get(k);

            boolean packageEmitted = false;
            for (String testName : testNames) {
                TestInfo test = TestList.getInfo(testName);
                TestResult result = results.get(testName);
                TestGrading grading = new TestGrading(result, test);
                if (grading.hasSpec) {
                    if (!packageEmitted) {
                        emitPackage(output, k);
                        packageEmitted = true;
                    }
                    emitTest(output, result, test);
                    hadAnyTests = true;
                }
            }
        }

        output.println("</table>");
        if (!hadAnyTests) {
            output.println("None!");
            output.println("<br>");
        }

        output.println("<br>");
    }

    private void printAllTests(Map<String, TestResult> results, Multimap<String, String> packages, PrintWriter output) throws FileNotFoundException, JAXBException {
        output.println("<h3>ALL tests:</h3>");
        output.println("<table cellspacing=0 cellpadding=0 width=\"100%\">\n" +
                "<tr>\n" +
                " <th class=\"header\">Test</th>\n" +
                " <th class=\"header\">Cycles</th>\n" +
                " <th class=\"header\">Results</th>\n" +
                "</tr>");

        for (String k : packages.keys()) {
            emitPackage(output, k);

            Collection<String> testNames = packages.get(k);
            for (String testName : testNames) {
                TestInfo test = TestList.getInfo(testName);
                TestResult result = results.get(testName);
                if (result.status() == Status.NORMAL) {
                    emitTest(output, result, test);
                } else {
                    emitTestFailure(output, result, test);
                }

                PrintWriter local = new PrintWriter(resultDir + "/" + testName + ".html");
                parseTest(local, result, test);
                local.close();
            }
        }

        output.println("</table>");
    }

    private void emitPackage(PrintWriter pw, String pack) {
        pw.println("<tr class=\"section2\">\n" +
                "   <td><b>" + pack + "</b></td>\n" +
                "   <td>&nbsp;</td>\n" +
                "   <td>&nbsp;</td>\n" +
                "   <td>&nbsp;</td>\n" +
                "   <td>&nbsp;</td>\n" +
                "   <td></td>" +
                "</tr>");
    }

    public static String cutKlass(String fqname) {
        return fqname.substring(fqname.lastIndexOf(".") + 1);
    }

    public void emitTest(PrintWriter output, TestResult result, TestInfo description) throws FileNotFoundException, JAXBException {
        cellStyle = 3 - cellStyle;
        output.println("<tr class=\"cell" + cellStyle + "\">");
        output.println("<td>&nbsp;&nbsp;&nbsp;<a href=\"" + result.getName() + ".html\">" + cutKlass(result.getName()) + "</a></td>");
        output.printf("<td>> 10<sup>%d</sup></td>", getRoughCount(result));
        if (description != null) {
            TestGrading grading = new TestGrading(result, description);
            if (grading.isPassed) {
                output.println("<td class=\"passed\">PASSED</td>");
            } else {
                output.println("<td class=\"failed\">FAILED</td>");
            }

            if (grading.hasInteresting) {
                output.println("<td class=\"interesting\">INTERESTING</td>");
            } else {
                output.println("<td class=\"interesting\"></td>");
            }
            if (grading.hasSpec) {
                output.println("<td class=\"spec\">SPEC</td>");
            } else {
                output.println("<td class=\"spec\"></td>");
            }
            output.println("<td class=\"passed\"></td>");
        } else {
            output.println("<td class=\"failed\">MISSING DESCRIPTION</td>");
            output.println("<td class=\"failed\"></td>");
            output.println("<td class=\"failed\"></td>");
            output.println("<td class=\"failed\"></td>");
        }
        output.println("</tr>");
    }

    public void emitTestFailure(PrintWriter output, TestResult result, TestInfo description) throws FileNotFoundException, JAXBException {
        cellStyle = 3 - cellStyle;
        output.println("<tr class=\"cell" + cellStyle + "\">");
        output.println("<td>&nbsp;&nbsp;&nbsp;<a href=\"" + result.getName() + ".html\">" + cutKlass(result.getName()) + "</a></td>");
        output.println("<td></td>");
        if (description != null) {
            switch (result.status()) {
                case API_MISMATCH:
                    output.println("<td class=\"interesting\">API MISMATCH</td>");
                    output.println("<td class=\"interesting\"></td>");
                    output.println("<td class=\"interesting\"></td>");
                    output.println("<td class=\"interesting\">Sanity check failed, API mismatch?</td>");
                    break;
                case TEST_ERROR:
                case CHECK_TEST_ERROR:
                    output.println("<td class=\"failed\">ERROR</td>");
                    output.println("<td class=\"failed\"></td>");
                    output.println("<td class=\"failed\"></td>");
                    output.println("<td class=\"failed\">Error while running the test</td>");
                    break;
                case TIMEOUT_ERROR:
                    output.println("<td class=\"failed\">ERROR</td>");
                    output.println("<td class=\"failed\"></td>");
                    output.println("<td class=\"failed\"></td>");
                    output.println("<td class=\"failed\">Timeout while running the test</td>");
                    break;
                case VM_ERROR:
                    output.println("<td class=\"failed\">VM ERROR</td>");
                    output.println("<td class=\"failed\"></td>");
                    output.println("<td class=\"failed\"></td>");
                    output.println("<td class=\"failed\">Error running the VM</td>");
                    break;
            }
        } else {
            output.println("<td class=\"failed\">MISSING DESCRIPTION</td>");
            output.println("<td class=\"failed\"></td>");
            output.println("<td class=\"failed\"></td>");
            output.println("<td class=\"failed\"></td>");
        }
        output.println("</tr>");
    }

    public static int getRoughCount(TestResult r) {
        long sum = 0;
        for (State s : r.getStates()) {
            sum += s.getCount();
        }

        return (int) Math.floor(Math.log10(sum));
    }


    public void parseTest(PrintWriter output, TestResult r, TestInfo test) throws FileNotFoundException, JAXBException {
        if (test == null) {
            parseTestWithoutDescription(output, r);
            return;
        }

        output.println("<h1>" + r.getName() + "</h1>");

        output.println("<p>" + test.description() + "</p>");

        int rIdx = 1;
        for (String ref : test.refs()) {
            output.println("<a href=\"" + ref + "\">[" + rIdx + "]</a>");
            rIdx++;
        }

        output.println("<table width=100%>");
        output.println("<tr>");
        output.println("<th width=250>Observed state</th>");
        output.println("<th width=50>Occurrence</th>");
        output.println("<th width=50>Expectation</th>");
        output.println("<th>Interpretation</th>");
        output.println("</tr>");

        List<State> unmatchedStates = new ArrayList<>();
        unmatchedStates.addAll(r.getStates());
        for (StateCase c : test.cases()) {

            boolean matched = false;

            for (State s : r.getStates()) {
                if (c.state().equals(s.getId())) {
                    // match!
                    output.println("<tr bgColor=" + selectHTMLColor(c.expect(), s.getCount() == 0) + ">");
                    output.println("<td>" + s.getId() + "</td>");
                    output.println("<td align=center>" + s.getCount() + "</td>");
                    output.println("<td align=center>" + c.expect() + "</td>");
                    output.println("<td>" + c.description() + "</td>");
                    output.println("</tr>");
                    matched = true;
                    unmatchedStates.remove(s);
                }
            }

            if (!matched) {
                output.println("<tr bgColor=" + selectHTMLColor(c.expect(), true) + ">");
                output.println("<td>" + c.state() + "</td>");
                output.println("<td align=center>" + 0 + "</td>");
                output.println("<td align=center>" + c.expect() + "</td>");
                output.println("<td>" + c.description() + "</td>");
                output.println("</tr>");
            }
        }

        for (State s : unmatchedStates) {
            output.println("<tr bgColor=" + selectHTMLColor(test.unmatched().expect(), s.getCount() == 0) + ">");
            output.println("<td>" + s.getId() + "</td>");
            output.println("<td align=center>" + s.getCount() + "</td>");
            output.println("<td align=center>" + test.unmatched().expect() + "</td>");
            output.println("<td>" + test.unmatched().expect() + "</td>");
            output.println("</tr>");
        }

        output.println("</table>");

        if (!r.getAuxData().isEmpty()) {
            output.println("<p><b>Auxiliary data</b></p>");
            output.println("<pre>");
            for (String data : r.getAuxData()) {
                output.println(data);
            }
            output.println("</pre>");
            output.println();
        }
    }

    private void parseTestWithoutDescription(PrintWriter output, TestResult r) {
        output.println("<h1>" + r.getName() + "</h1>");

        output.println("<p>No description available for this test</p>");

        output.println("<table width=100%>");
        output.println("<tr>");
        output.println("<th width=250>Observed state</th>");
        output.println("<th width=50>Occurrence</th>");
        output.println("<th width=50>Expectation</th>");
        output.println("<th>Interpretation</th>");
        output.println("<th width=50>Refs</th>");
        output.println("</tr>");

        for (State s : r.getStates()) {
            output.println("<tr bgColor=" + selectHTMLColor(Expect.UNKNOWN, s.getCount() == 0) + ">");
            output.println("<td>" + s.getId() + "</td>");
            output.println("<td align=center>" + s.getCount() + "</td>");
            output.println("<td align=center>" + Expect.UNKNOWN + "</td>");
            output.println("<td>" + "Unknows state" + "</td>");
            output.println("</tr>");
        }
        output.println("</table>");
    }

    public String selectHTMLColor(Expect type, boolean isZero) {
        String rgb = Integer.toHexString(selectColor(type, isZero).getRGB());
        return "#" + rgb.substring(2, rgb.length());
    }

    public Color selectColor(Expect type, boolean isZero) {
        switch (type) {
            case ACCEPTABLE:
                return isZero ? Color.LIGHT_GRAY : Color.GREEN;
            case FORBIDDEN:
                return isZero ? Color.LIGHT_GRAY : Color.RED;
            case ACCEPTABLE_INTERESTING:
                return isZero ? Color.LIGHT_GRAY : Color.CYAN;
            case ACCEPTABLE_SPEC:
                return isZero ? Color.LIGHT_GRAY : Color.YELLOW;
            case UNKNOWN:
                return Color.RED;
            default:
                throw new IllegalStateException();
        }
    }

}
