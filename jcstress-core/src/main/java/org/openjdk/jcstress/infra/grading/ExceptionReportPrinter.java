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
import org.openjdk.jcstress.infra.Status;
import org.openjdk.jcstress.infra.TestInfo;
import org.openjdk.jcstress.infra.collectors.InProcessCollector;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.runners.TestConfig;
import org.openjdk.jcstress.infra.runners.TestList;
import org.openjdk.jcstress.util.*;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Exception report.
 *
 * Throws deferred test exceptions, if any.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class ExceptionReportPrinter {

    private final List<String> failures;
    private final InProcessCollector collector;

    public ExceptionReportPrinter(InProcessCollector collector) throws JAXBException, FileNotFoundException {
        this.collector = collector;
        failures = new ArrayList<>();
    }

    public void parse() throws FileNotFoundException, JAXBException {
        List<TestResult> results = ReportUtils.mergedByConfig(collector.getTestResults());

        for (TestResult k : results) {
            emitTest(k);
        }

        if (!failures.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String f : failures) {
                sb.append(f).append("\n");
            }
            throw new AssertionError("TEST FAILURES: \n" + sb.toString());
        }
    }

    public void emitTest(TestResult result) throws FileNotFoundException, JAXBException {
        switch (result.status()) {
            case CHECK_TEST_ERROR:
                failures.add(result.getName() + " had failed with the pre-test error.");
                break;
            case TEST_ERROR:
                failures.add(result.getName() + " had failed with the test error.");
                break;
            case TIMEOUT_ERROR:
                failures.add(result.getName() + " had timed out.");
                break;
            case VM_ERROR:
                failures.add(result.getName() + " had failed with the VM error.");
                break;
            case NORMAL:
                TestGrading grading = TestGrading.grade(result);
                if (!grading.failureMessages.isEmpty()) {
                    for (String msg : grading.failureMessages) {
                        failures.add(result.getName() + ": " + msg);
                    }
                }
                break;
            case API_MISMATCH:
                // silently ignore
                break;
            default:
                throw new IllegalStateException("Unhandled status: " + result.status());
        }
    }

}
