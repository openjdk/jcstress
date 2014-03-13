/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestList {

    public static final String LIST = "/META-INF/TestList";

    private static volatile Map<String, String> tests;

    private static Map<String, String> getTests() {
        if (tests == null) {
            Map<String, String> m = new HashMap<String, String>();
            InputStream stream = null;
            try {
                stream = TestList.class.getResourceAsStream(LIST);
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] ls = line.split(",");
                    m.put(ls[0], ls[1]);
                }
            } catch (IOException e) {
                // FIXME: Print warning
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

    public static String getRunner(String test) {
        return tests.get(test);
    }
}
