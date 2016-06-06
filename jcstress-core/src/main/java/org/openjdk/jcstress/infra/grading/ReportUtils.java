/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jcstress.infra.Status;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.runners.TestConfig;
import org.openjdk.jcstress.util.*;

import java.util.*;

public class ReportUtils {

    public static List<TestResult> mergedByConfig(Collection<TestResult> src) {
        Multimap<TestConfig, TestResult> multiResults = new HashMultimap<>();
        for (TestResult r : src) {
            multiResults.put(r.getConfig(), r);
        }

        List<TestResult> results = new ArrayList<>();
        for (TestConfig config : multiResults.keys()) {
            Collection<TestResult> mergeable = multiResults.get(config);
            TestResult root = merged(config, mergeable);
            results.add(root);
        }

        return results;
    }

    public static List<TestResult> mergedByName(Collection<TestResult> src) {
        Multimap<String, TestResult> multiResults = new HashMultimap<>();
        for (TestResult r : src) {
            multiResults.put(r.getConfig().name, r);
        }

        List<TestResult> results = new ArrayList<>();
        for (String name : multiResults.keys()) {
            Collection<TestResult> mergeable = multiResults.get(name);
            TestResult root = merged(mergeable.iterator().next().getConfig(), mergeable);
            results.add(root);
        }

        return results;
    }

    public static Multimap<String, TestResult> byName(Collection<TestResult> src) {
        Multimap<String, TestResult> result = new HashMultimap<>();
        for (TestResult r : mergedByConfig(src)) {
            result.put(r.getName(), r);
        }
        return result;
    }

    private static TestResult merged(TestConfig config, Collection<TestResult> mergeable) {
        Multiset<String> stateCounts = new HashMultiset<>();

        List<String> auxData = new ArrayList<>();

        Status status = Status.NORMAL;
        Environment env = null;
        for (TestResult r : mergeable) {
            status = status.combine(r.status());
            for (String s : r.getStateKeys()) {
                stateCounts.add(s, r.getCount(s));
            }
            env = r.getEnv();
            auxData.addAll(r.getAuxData());
        }

        TestResult root = new TestResult(config, status, 0);

        for (String s : stateCounts.keys()) {
            root.addState(s, stateCounts.count(s));
        }

        root.setEnv(env);

        for (String data : auxData) {
            root.addAuxData(data);
        }
        return root;
    }


}
