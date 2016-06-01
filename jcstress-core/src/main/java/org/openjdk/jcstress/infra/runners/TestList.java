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
package org.openjdk.jcstress.infra.runners;

import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.infra.StateCase;
import org.openjdk.jcstress.infra.TestInfo;
import org.openjdk.jcstress.util.TestLineReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TestList {

    public static final String LIST = "/META-INF/TestList";

    private static volatile Map<String, TestInfo> tests;

    private static Map<String, TestInfo> getTests() {
        if (tests == null) {
            Map<String, TestInfo> m = new HashMap<String, TestInfo>();
            InputStream stream = null;
            try {
                stream = TestList.class.getResourceAsStream(LIST);
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

                String line;
                while ((line = reader.readLine()) != null) {
                    TestLineReader read = new TestLineReader(line);

                    if (read.isCorrect()) {
                        String name = read.nextString();
                        String runner = read.nextString();
                        String description = read.nextString();
                        int actorCount = read.nextInt();
                        boolean requiresFork = read.nextBoolean();
                        int caseCount = read.nextInt();

                        TestInfo testInfo = new TestInfo(name, runner, description, actorCount, requiresFork);
                        m.put(name, testInfo);

                        for (int c = 0; c < caseCount; c++) {
                            Expect expect  = Expect.valueOf(read.nextString());
                            String desc    = read.nextString();
                            int stateCount = read.nextInt();
                            for (int s = 0; s < stateCount; s++) {
                                String regex = read.nextString();
                                Pattern pattern = Pattern.compile(regex);
                                testInfo.addCase(new StateCase(pattern, expect, desc));
                            }
                        }

                        int refCount = read.nextInt();
                        for (int c = 0; c < refCount; c++) {
                            testInfo.addRef(read.nextString());
                        }
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Fatal error", e);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        // swallow
                    }
                }
            }
            tests = m;
        }
        return tests;
    }


    public static Collection<String> tests() {
        return getTests().keySet();
    }

    public static TestInfo getInfo(String name) {
        return getTests().get(name);
    }
}
