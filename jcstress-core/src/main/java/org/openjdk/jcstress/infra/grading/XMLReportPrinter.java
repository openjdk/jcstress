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
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Prints HTML reports.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class XMLReportPrinter {

    public static final String SOFT_ERROR_AS = "jcstress.report.xml.softErrorAs"; //pass/fail defaults to pass
    public static final String HARD_ERROR_AS = "jcstress.report.xml.hardErrorAs"; //pass/fail defaults to fail
    public static final String USE_TESTSUITES = "jcstress.report.xml.sparse.testsuites";
    public static final String TESTSUITES_STRIPNAMES = "jcstress.report.xml.sparse.stripNames";
    public static final String DUPLICATE_PROPERTIES = "jcstress.report.xml.properties.dupliate";
    public static final String STDOUTERR_TO_FAILURE = "jcstress.report.xml.souterr2failure";
    public static final String VALIDATE = "jcstress.report.xml.validate";
    public static final String NO_COMMENTS = "jcstress.report.xml.nocomments";
    public static final String SPARSE = "jcstress.report.xml.sparse";
    private final String resultDir;
    private final InProcessCollector collector;
    private final boolean sparse;
    private final PrintStream out;

    public XMLReportPrinter(String resultDir, InProcessCollector collector, PrintStream out, boolean sparse) {
        //sparse true -ALL_MATCHING
        //sparse false - as ALL_MATCHING_COMBINATIONS
        //jednou smichat, jednou ne. Varovani kolik jich bude
        //-xml true/false, defaults to sparse
        this.collector = collector;
        this.resultDir = resultDir;
        this.sparse = sparse;
        File dir = new File(resultDir);
        dir.mkdirs();
        out.println("  " + getSparseString() + " XML report generated at " + dir.getAbsolutePath() + File.separator + getMainFileName());
        this.out = out;
    }

    private static ErrorAs getSoftErrorAs() {
        if (System.getProperty(XMLReportPrinter.SOFT_ERROR_AS) == null) {
            return ErrorAs.pass;
        }
        return Enum.valueOf(ErrorAs.class, System.getProperty(XMLReportPrinter.SOFT_ERROR_AS));
    }

    private static ErrorAs getHardErrorAs() {
        if (System.getProperty(XMLReportPrinter.HARD_ERROR_AS) == null) {
            return ErrorAs.fail;
        }
        return Enum.valueOf(ErrorAs.class, System.getProperty(XMLReportPrinter.HARD_ERROR_AS));
    }

    public static Boolean getSparse(PrintStream out) {
        String sparse = System.getProperty(SPARSE);
        if (sparse == null) {
            return null;
        } else {
            if ("true".equals(sparse) || "false".equals(sparse )) {
                return Boolean.getBoolean(XMLReportPrinter.SPARSE);
            } else {
                out.println("Invalid " + SPARSE + " value of " + sparse + "Should be true/false or missing");
                return null;
            }
        }
    }


    private static boolean isTestsuiteUsed() {
        return System.getProperty(XMLReportPrinter.USE_TESTSUITES) != null;
    }

    private static boolean isStdoutErrToFailure() {
        return System.getProperty(XMLReportPrinter.STDOUTERR_TO_FAILURE) != null;
    }

    private static boolean isStripNames() {
        return System.getProperty(XMLReportPrinter.TESTSUITES_STRIPNAMES) != null;
    }

    private static boolean isValidate() {
        return System.getProperty(XMLReportPrinter.VALIDATE) != null;
    }

    private static boolean isDuplicateProperties() {
        return System.getProperty(XMLReportPrinter.DUPLICATE_PROPERTIES) != null;
    }

    private static boolean isNoComments() {
        return System.getProperty(XMLReportPrinter.NO_COMMENTS) != null;
    }

    private static void printBaseProperties(List<TestResult> sorted, PrintWriter o) {
        for (Map.Entry<String, String> entry : HTMLReportPrinter.getEnv(sorted).entrySet()) {
            o.println("          <property name='" + entry.getKey() + "' value='" + entry.getValue() + "'/>");
        }
    }

    private static String getRoughCount(TestResult r) {
        long sum = r.getTotalCount();
        if (sum > 10) {
            return "10^" + (int) Math.floor(Math.log10(sum));
        } else {
            return String.valueOf(sum);
        }
    }

    private static void printSeed(PrintWriter o, TestResult r) {
        if (r.getConfig().getSeed() != null) {
            o.println("          <property name='seed' value='" + r.getConfig().getSeed() + "'/>");
        }
    }

    private static void printRefs(PrintWriter o, TestInfo test) {
        for (String ref : test.refs()) {
            if (ref != null) {
                o.println("          <property name='bug' value='" + ref + "'/>");
            }
        }
    }

    private static void printDescription(PrintWriter o, TestInfo test) {
        if (test.description() != null) {
            o.println("          <property name='description' value='" + test.description() + "'/>");
        }
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

        String filePath = resultDir + File.separator + getMainFileName();
        PrintWriter output = new PrintWriter(filePath);

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

            String hostname = "localhost";
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (Exception ex) {
                //no interest
            }
            output.println("<?xml version='1.0' encoding='UTF-8'?>");
            //in case of testsuites used
            //consuilt <testsuites name="Test run" tests="8" failures="1" errors="1" skipped="1" time="16.082687" timestamp="2021-04-02T15:48:23">
            //check whether both writings ar eok for jtreg plugin
            output.println("<testsuite name='jcstress'" +
                    " tests='" + totalCount + "'" +
                    " failures='" + failedCount + "'" +
                    " errors='" + 0/*fixme*/ + "'" +
                    " skipped='" + sanityFailedCount + "' " +
                    " time='" + 0/*fixme*/ + "'" +
                    " timestamp='" + new Date().toString() +  "' " +
                    " hostname='" + hostname + "'>");

        }
        {

            output.println("  <properties>");
            printBaseProperties(byName, output);
            output.println("    <property name='sparse' value='" + sparse + "' />");
            output.println("    <property name='" + USE_TESTSUITES + "' value='" + isTestsuiteUsed() + "' />");
            output.println("    <property name='" + TESTSUITES_STRIPNAMES + "' value='" + isStripNames() + "' />");
            output.println("    <property name='" + SOFT_ERROR_AS + "' value='" + getSoftErrorAs() + "' />");
            output.println("    <property name='" + HARD_ERROR_AS + "' value='" + getHardErrorAs() + "' />");
            output.println("    <property name='" + DUPLICATE_PROPERTIES + "' value='" + isDuplicateProperties() + "' />");
            output.println("    <property name='" + NO_COMMENTS + "' value='" + isNoComments() + "' />");
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
        output.println("</testsuite>");
        output.flush();
        output.close();
        if (isValidate()) {
            validate(filePath);
        }
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

    private void emitTest(PrintWriter output, TestResult result) {
        TestGrading grading = result.grading();
        if (grading.isPassed) {
            output.println("  Passed - " + StringUtils.chunkName(result.getName()) + " " + getRoughCount(result));
        } else {
            output.println("  FAILED - " + StringUtils.chunkName(result.getName()) + " " + getRoughCount(result));
        }

        if (grading.hasInteresting) {
            output.println("    was interesting");
        }
    }

    private void emitTestFailure(PrintWriter output, TestResult result) {
        output.println("   FAILED - " + StringUtils.chunkName(result.getName()) + " " + getRoughCount(result));
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

    private void emitTestReports(Multimap<String, TestResult> multiByName, PrintWriter local) {
        multiByName.keys().stream().forEach(name -> {
            TestInfo test = TestList.getInfo(name);
            emitTestReport(local, multiByName.get(name), test, name);
        });
    }

    private void emitTestReport(PrintWriter o, Collection<TestResult> results, TestInfo test, String suiteCandidate) {
        //in sparse mode we print only test.name as test, with result based on cumulative
        //otherwise we will be printing only its individual combinations (to mach the summary)
        if (sparse) {
            List<TestResult> sorted = new ArrayList<>(results);
            HTMLReportPrinter.resultsOrder(sorted);
            o.println("  <testcase class='jcstress' name='" + test.name() + "'>");
            o.println("      <properties>");
            printDescription(o, test);
            printRefs(o, test);
            if (isDuplicateProperties()) {
                printBaseProperties(sorted, o);
            }
            o.println("      </properties>");
            Set<String> keys = new TreeSet<>();
            for (TestResult r : sorted) {
                keys.addAll(r.getStateKeys());
            }
            o.println("<failure><![CDATA["); //or error //or <!-- if pass?
            for (TestResult r : sorted) {
                o.println(r.getConfig().toDetailedTest(true));
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
            }
            o.println("]]></failure>");//or error //or <!-- if pass?

            o.println("<system-out><![CDATA[");
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
            o.println("]]></system-out>");
            o.println("<system-err><![CDATA[");
            for (TestResult r : sorted) {
                if (!r.getVmErr().isEmpty()) {
                    resultHeader(o, r);
                    TestConfig cfg = r.getConfig();
                    for (String data : r.getVmErr()) {
                        o.println(data);
                    }
                    o.println();
                }
            }
            o.println("]]></system-err>\n");

            o.println("</testcase>");
        } else {
            if (isTestsuiteUsed()) {
                o.println("  <testsuite name='"+suiteCandidate + "'>");
            }
            List<TestResult> sorted = new ArrayList<>(results);
            HTMLReportPrinter.resultsOrder(sorted);
            for (TestResult r : sorted) {
                String testName=r.getConfig().toDetailedTest(false);
                if (isTestsuiteUsed() && isStripNames()) {
                    testName=r.getConfig().getTestVariant(false);
                }
                o.println("  <testcase class='jcstress' name='" + testName + "'>");
                o.println("      <properties>");
                printDescription(o, test);
                printRefs(o, test);
                printSeed(o, r);
                if (isDuplicateProperties()) {
                    printBaseProperties(sorted, o);
                }

                o.println("      </properties>");

                Set<String> keys = new TreeSet<>();
                keys.addAll(r.getStateKeys());
                o.println("<failure><![CDATA[");//or error //or <!-- if pass?

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
                o.println("]]></failure>");//or error //or <!-- if pass?
                o.println("<system-out><![CDATA[");
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
                o.println("]]></system-out>");
                o.println("<system-err><![CDATA[");
                if (!r.getVmErr().isEmpty()) {
                    resultHeader(o, r);
                    for (String data : r.getVmErr()) {
                        o.println(data);
                    }
                    o.println();
                }
                o.println("]]></system-err>\n");

                o.println("</testcase>");
            }
            if (isTestsuiteUsed()) {
                o.println("  </testsuite>");
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

    private String selectColor(Expect type, boolean isZero) {
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

    private void validate(String xml) {
        try {
            out.println("Checking: " + xml);
            wellFormed(xml);
            validByXsd(xml);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void wellFormed(String xml) throws ParserConfigurationException, SAXException, IOException {
        out.println("Well formed?");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(xml));
        out.println("Well formed!");
    }

    private void validByXsd(String xml) throws ParserConfigurationException, SAXException, IOException, URISyntaxException {
        String url = "https://raw.githubusercontent.com/junit-team/junit5/refs/heads/main/platform-tests/src/test/resources/jenkins-junit.xsd";
        out.println("Valid by " + url + " ?");
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(new URI(url).toURL());
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(new File(xml)));
        out.println("Valid!");
    }

    private enum ErrorAs {
        error, fail, pass
    }

}
