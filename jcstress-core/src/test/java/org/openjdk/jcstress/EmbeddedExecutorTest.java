/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jcstress.infra.Status;
import org.openjdk.jcstress.infra.TestInfo;
import org.openjdk.jcstress.infra.collectors.InProcessCollector;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.runners.TestConfig;
import org.openjdk.jcstress.vm.CompileMode;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.openjdk.jcstress.util.StringUtils.getFirstLine;

public class EmbeddedExecutorTest {

    private InProcessCollector sink = new InProcessCollector();

    private EmbeddedExecutor embeddedExecutor = new EmbeddedExecutor(sink);

    @Test
    public void testReportTestError() throws IOException {
        TestConfig config = createSimpleConfigFor("my.missing.ClassName");
        embeddedExecutor.run(config);

        Assert.assertEquals("Should get exactly one test result", 1, sink.getTestResults().size());

        TestResult testResult = sink.getTestResults().iterator().next();
        Assert.assertEquals("Should report a test error", Status.TEST_ERROR, testResult.status());

        String errorMessage = testResult.getMessages().get(0);
        Assert.assertEquals("Should report the test error reason", "java.lang.ClassNotFoundException: my.missing.ClassName",
                getFirstLine(errorMessage));
    }

    private TestConfig createSimpleConfigFor(String runnerClassName) throws IOException {
        Options opts = new Options(new String[]{});
        opts.parse();
        TestInfo info = new TestInfo("", runnerClassName, "", 4, Arrays.asList("a1", "a2", "a3", "a4"), false);
        return new TestConfig(opts, info, TestConfig.RunMode.FORKED, 1, Collections.emptyList(), CompileMode.UNIFIED);
    }

}