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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Prints HTML reports.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class XMLReportPrinter {

    private enum JunitResult {
        pass, failure, error, skipped
    }

    //how to deal with sofrt errors like api mishmash or similar
    public static final String SOFT_ERROR_AS = "jcstress.report.xml.softErrorAs"; //pass/fail/skip defaults to skip
    //how to deal with hard errors. Those may be timout, but also segfaulting vm
    public static final String HARD_ERROR_AS = "jcstress.report.xml.hardErrorAs"; //pass/fail defaults to fail
    //only for full (non-saprse) output, will wrap each family by its <testsuite name> FIXME missing statistics/counters
    public static final String USE_TESTSUITES = "jcstress.report.xml.sparse.testsuites";
    //in case of sued testsuiotes, will not replicate the name of suite in test name.
    //it is not stripped by default for sake of comparisns
    public static final String TESTSUITES_STRIPNAMES = "jcstress.report.xml.sparse.stripNames";
    //will repritn system and jvm info in each test (may significantly waste sapce)
    public static final String DUPLICATE_PROPERTIES = "jcstress.report.xml.properties.dupliate";
    //will move stdout/err to failure/error message for failures/errors and omit for passes
    //this is for tools,m which do nto show stdout/err properly
    //also it is saving a bit of space, but is loosing the granularity
    public static final String STDOUTERR_TO_FAILURE = "jcstress.report.xml.souterr2failure";
    //vill validate final xmls
    public static final String VALIDATE = "jcstress.report.xml.validate";
    //will nto include comments (if any)
    public static final String NO_COMMENTS = "jcstress.report.xml.nocomments";
    //by default both reprots are printed. By setting it to true or false, wil linclude only sparse ot full
    public static final String SPARSE = "jcstress.report.xml.sparse"; //true/false/null

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

    private static JunitResult getSoftErrorAs() {
        if (System.getProperty(XMLReportPrinter.SOFT_ERROR_AS) == null) {
            return JunitResult.skipped;
        }
        return Enum.valueOf(JunitResult.class, System.getProperty(XMLReportPrinter.SOFT_ERROR_AS));
    }

    private static JunitResult getHardErrorAs() {
        if (System.getProperty(XMLReportPrinter.HARD_ERROR_AS) == null) {
            return JunitResult.failure;
        }
        return Enum.valueOf(JunitResult.class, System.getProperty(XMLReportPrinter.HARD_ERROR_AS));
    }

    public static Boolean getSparse(PrintStream out) {
        String sparse = System.getProperty(SPARSE);
        if (sparse == null) {
            return null;
        } else {
            if ("true".equals(sparse) || "false".equals(sparse)) {
                return Boolean.getBoolean(XMLReportPrinter.SPARSE);
            } else {
                if (out != null) {
                    out.println("Invalid " + SPARSE + " value of " + sparse + "Should be true/false or missing");
                }
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

    private static String printProperty(String key, boolean value) {
        return printProperty(key, "" + value);
    }

    private static String printProperty(String key, String value) {
        return "<property name='" + key + "' value='" + value + "'/>";
    }

    private static String getBaseProperties(List<TestResult> sorted) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : HTMLReportPrinter.getEnv(sorted).entrySet()) {
            sb.append("          " + printProperty(entry.getKey(), entry.getValue())).append("\n");
        }
        return sb.toString();
    }


    private static String getSeed(TestResult r) {
        if (r.getConfig().getSeed() != null) {
            return ("          " + printProperty("seed", getCleanSeed(r)) + "\n");
        } else {
            return null;
        }
    }

    private static String getCleanSeed(TestResult r) {
        String[] args = r.getConfig().getSeed().split("=");
        if (args.length>1) {
            return args[1];
        }
        return args[0];
    }

    private static String getRefs(TestInfo test) {
        StringBuilder sb = new StringBuilder();
        for (String ref : test.refs()) {
            if (ref != null) {
                sb.append("          " + printProperty("bug", ref)).append("\n");
            }
        }
        return sb.toString();
    }

    private static String getTestDescription(TestInfo test) {
        if (test.description() != null && !test.description().equals("null")) {
            return "          " + printProperty("description", test.description() + "\n");
        } else {
            return null;
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
            //FIXME not honouring user setup! use junit result
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
                    " timestamp='" + new Date().toString() + "' " +
                    " hostname='" + hostname + "'>");

        }
        {

            output.println("  <properties>");
            output.print(getBaseProperties(byName));
            //FIXME print all properties, if 7903889 got ever implemented
            printXmlReporterProperties(output);
            output.println("  </properties>");
        }
// we have create dsummary, lets try to prnit the rest from merged info
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

    private void printXmlReporterProperties(PrintWriter output) {
        output.println("    " + printProperty("sparse", sparse));
        output.println("    " + printProperty(USE_TESTSUITES, isTestsuiteUsed()));
        output.println("    " + printProperty(TESTSUITES_STRIPNAMES, isStripNames()));
        output.println("    " + printProperty(SOFT_ERROR_AS, getSoftErrorAs().toString()));
        output.println("    " + printProperty(HARD_ERROR_AS, getHardErrorAs().toString()));
        output.println("    " + printProperty(DUPLICATE_PROPERTIES, isDuplicateProperties()));
        output.println("    " + printProperty(NO_COMMENTS, isNoComments()));
        output.println("    " + printProperty(STDOUTERR_TO_FAILURE, isStdoutErrToFailure()));
        output.println("    " + printProperty(SPARSE, Objects.toString(getSparse(null))));
    }

    private void emitTestReports(Multimap<String, TestResult> multiByName, PrintWriter local) {
        multiByName.keys().stream().forEach(name -> {
            TestInfo test = TestList.getInfo(name);
            emitTestReport(local, multiByName.get(name), test, name);
        });
    }

    private void emitTestReport(PrintWriter outw, Collection<TestResult> results, TestInfo test, String suiteCandidate) {
        //in sparse mode we print only test.name as test, with result based on cumulative
        //otherwise we will be printing only its individual combinations (to mach the summary)
        if (sparse) {
            List<TestResult> sorted = new ArrayList<>(results);
            HTMLReportPrinter.resultsOrder(sorted);
            outw.println("  <testcase class='jcstress' name='" + test.name() + "'>");
            printPropertiesPerTest(outw, test, null, sorted);
            printMainTestBody(outw, sorted, true);
            outw.println("</testcase>");
        } else {
            if (isTestsuiteUsed()) {
                outw.println("  <testsuite name='" + suiteCandidate + "'>");
            }
            List<TestResult> sorted = new ArrayList<>(results);
            HTMLReportPrinter.resultsOrder(sorted);
            for (TestResult r : sorted) {
                String testName = r.getConfig().toDetailedTest(false);
                if (isTestsuiteUsed() && isStripNames()) {
                    testName = r.getConfig().getTestVariant(false);
                }
                outw.println("  <testcase class='jcstress' name='" + testName + "'>");
                printPropertiesPerTest(outw, test, r, sorted);
                printMainTestBody(outw, Arrays.asList(r), null);
                outw.println("</testcase>");
            }
            if (isTestsuiteUsed()) {
                outw.println("  </testsuite>");
            }
        }

    }

    private static void printPropertiesPerTest(PrintWriter outw, TestInfo test, TestResult result, List<TestResult> sorted) {
        String props = printPropertiesPerTest(test, result, sorted);
        if (!props.isEmpty()) {
            outw.println("      <properties>");
            outw.print(props);
            outw.println("      </properties>");
        }
    }

    private static String printPropertiesPerTest(TestInfo test, TestResult result, List<TestResult> sorted) {
        StringBuilder sb = new StringBuilder();
        String description = getTestDescription(test);
        if (description != null && !description.isEmpty()) {
            sb.append(description);
        }
        String refs = getRefs(test);
        if (refs != null && !refs.isEmpty()) {
            sb.append(refs);
        }
        if (result != null) {
            String seed = getSeed(result);
            if (seed != null && !seed.isEmpty()) {
                sb.append(seed);
            }
        }
        if (isDuplicateProperties()) {
            String baseProps = getBaseProperties(sorted);
            if (baseProps != null && !baseProps.isEmpty()) {
                sb.append(baseProps);
            }
        }
        return sb.toString();
    }

    private static void printMainTestBody(PrintWriter outw, List<TestResult> results, Boolean header) {
        Set<String> keys = new TreeSet<>();
        for (TestResult result : results) {
            keys.addAll(result.getStateKeys());
        }
        printStatusElement(outw, results, keys, header);
        if (!isStdoutErrToFailure()) {
            printSystemOutElement(outw, results, header);
            printSystemErrElement(outw, results, header);
        }
    }

    private static void printStatusElement(PrintWriter outw, List<TestResult> results, Set<String> keys, Boolean header) {
        JunitResult junitResult = testsToJunitResult(results);
        if (junitResult == JunitResult.failure || junitResult == JunitResult.error) {
            outw.println("<" + junitResult + "><![CDATA[");
            for (TestResult result : results) {
                outw.println(result.getConfig().toDetailedTest(true));
                printHtmlInfo(result, outw, keys);
                if (isStdoutErrToFailure()) {
                    printMessages(outw, result, header);
                    printVmOut(outw, result, null);
                    printVmErr(outw, result, header);
                }
            }
            outw.println("]]></" + junitResult + ">");
        }
        if (junitResult == JunitResult.skipped) {
            outw.println(" <skipped message='api missmastch?' />");
        }
    }

    private static void printSystemErrElement(PrintWriter outw, Collection<TestResult> results, Boolean header) {
        outw.println("<system-err><![CDATA[");
        for (TestResult r : results) {
            printVmErr(outw, r, header);
        }
        outw.println("]]></system-err>\n");
    }

    private static void printSystemOutElement(PrintWriter outw, Collection<TestResult> results, Boolean header) {
        outw.println("<system-out><![CDATA[");
        for (TestResult r : results) {
            printMessages(outw, r, header);
            printVmOut(outw, r, null);
        }
        outw.println("]]></system-out>");
    }

    private static void printVmErr(PrintWriter outw, TestResult result, Boolean header) {
        printTestLines(result.getVmErr(), outw, result, header);
    }

    private static void printVmOut(PrintWriter outw, TestResult result, Boolean header) {
        printTestLines(result.getVmOut(), outw, result, header);
    }

    private static void printMessages(PrintWriter outw, TestResult result, Boolean header) {
        printTestLines(result.getMessages(), outw, result, header);
    }

    private static void printTestLines(List<String> lines, PrintWriter outw, TestResult originalResult, Boolean header) {
        if (!lines.isEmpty()) {
            resultHeader(outw, originalResult, header);
            for (String data : lines) {
                outw.println(data);
            }
            outw.println();
        }
    }

    private static void printHtmlInfo(TestResult result, PrintWriter out, Set<String> keys) {
        String color = ReportUtils.statusToPassed(result) ? "green" : "red";
        String label = ReportUtils.statusToLabel(result);
        out.println("html signatue: " + color + " - " + label);
        getGradings(result, out, keys);
    }

    private static void getGradings(TestResult result, PrintWriter out, Set<String> keys) {
        for (String key : keys) {
            GradingResult c = result.grading().gradingResults.get(key);
            if (c != null) {
                out.println(selectHtmlGradingColor(c.expect, c.count == 0) + "/" + c.count + "");
            } else {
                out.println(selectHtmlGradingColor(Expect.ACCEPTABLE, true) + "/0");
            }
        }
    }

    private static void resultHeader(PrintWriter outw, TestResult r, Boolean full) {
        if (full != null) {
            TestConfig cfg = r.getConfig();
            if (full) {
                outw.println(cfg.toDetailedTest(false));
            } else {
                outw.println("CompileMode: " + CompileMode.description(cfg.compileMode, cfg.actorNames));
                outw.println("SchedulingClass" + SchedulingClass.description(cfg.shClass, cfg.actorNames));
                outw.println("");
                if (!cfg.jvmArgs.isEmpty()) {
                    outw.println("jvmargs:" + cfg.jvmArgs);
                }
            }
        }
    }

    private static String selectHtmlGradingColor(Expect type, boolean isZero) {
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

    public static JunitResult testsToJunitResult(Collection<TestResult> results) {
        boolean hadError = false;
        int coutSkipped = 0;
        for (TestResult result : results) {
            if (testToJunitResult(result) == JunitResult.failure) {
                //if there was failure in sub set, return whole group as failure
                return JunitResult.failure;
            }
            if (testToJunitResult(result) == JunitResult.error) {
                hadError = true;
            }
            if (testToJunitResult(result) == JunitResult.skipped) {
                coutSkipped++;
            }
        }
        //no failure, bute errors presented
        if (hadError) {
            return JunitResult.error;
        }
        //no failure, no error, was all skipped?
        if (coutSkipped == results.size()) {
            return JunitResult.skipped;
        }
        return JunitResult.pass;
    }

    public static JunitResult testToJunitResult(TestResult result) {
        switch (result.status()) {
            case TIMEOUT_ERROR:
                return JunitResult.error;
            case CHECK_TEST_ERROR:
                return JunitResult.error;
            case TEST_ERROR:
            case VM_ERROR:
                return getHardErrorAs();
            case API_MISMATCH:
                return getSoftErrorAs();
            case NORMAL:
                return JunitResult.pass;
            default:
                throw new IllegalStateException("Illegal status: " + result.status());
        }
    }

    /// /// /////////candidates to remove
    /// /// /////////candidates to remove
    /// /// /////////candidates to remove

    private static String getRoughCount(TestResult r) {
        long sum = r.getTotalCount();
        if (sum > 10) {
            return "10^" + (int) Math.floor(Math.log10(sum));
        } else {
            return String.valueOf(sum);
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

}