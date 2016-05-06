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
package org.openjdk.jcstress.chapters;

import org.openjdk.jcstress.Spp;
import org.openjdk.jcstress.Values;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Chapter0aTestGenerator {

    public static final String PREFIX = "org.openjdk.jcstress.tests";
    public static final String[] TYPES = new String[]{"byte", "boolean", "char", "short", "int", "float", "long", "double", "String"};

    public static void main(String... args) throws IOException {
        if (args.length < 1) {
            throw new IllegalStateException("Need a destination argument");
        }
        String dest = args[0];

        makeTests(
                dest,
                readFromResource("/chapter0a/X-FieldAtomicityTest.java.template"),
                "accessAtomic.fields",
                new String[]{ "", "volatile" }
        );

        makeTests(
                dest,
                readFromResource("/chapter0a/X-FieldDefaultValuesTest.java.template"),
                "defaultValues.fields",
                new String[]{ "", "volatile" }
        );

        makeTests(
                dest,
                readFromResource("/chapter0a/X-FieldInitTest.java.template"),
                "init.fields",
                new String[]{ "", "volatile", "final" }
        );

        makeTests(
                dest,
                readFromResource("/chapter0a/X-FieldTearingTest.java.template"),
                "tearing.fields",
                new String[]{ "", "volatile" }
        );

        makeTests(
                dest,
                readFromResource("/chapter0a/X-ArrayDefaultValuesTest.java.template"),
                "defaultValues.arrays.small",
                new String[]{ "", "volatile" }
        );

        makeTests(
                dest,
                readFromResource("/chapter0a/X-ArrayLargeDefaultValuesTest.java.template"),
                "defaultValues.arrays.large",
                new String[]{ "", "volatile" }
        );

        makeTests(
                dest,
                readFromResource("/chapter0a/X-ArrayInitTest.java.template"),
                "init.arrays.small",
                new String[]{ "", "volatile" }
        );

        makeTests(
                dest,
                readFromResource("/chapter0a/X-ArrayLargeInitTest.java.template"),
                "init.arrays.large",
                new String[]{ "", "volatile" }
        );

        makeTests(
                dest,
                readFromResource("/chapter0a/X-ArrayAtomicityTest.java.template"),
                "accessAtomic.arrays.small",
                new String[]{ "", "volatile" }
        );

        makeTests(
                dest,
                readFromResource("/chapter0a/X-ArrayLargeAtomicityTest.java.template"),
                "accessAtomic.arrays.large",
                new String[]{ "", "volatile" }
        );

        makeTests(
                dest,
                readFromResource("/chapter0a/X-ArrayTearingTest.java.template"),
                "tearing.arrays.small",
                new String[]{ "", "volatile" }
        );

        makeTests(
                dest,
                readFromResource("/chapter0a/X-ArrayLargeTearingTest.java.template"),
                "tearing.arrays.large",
                new String[]{ "", "volatile" }
        );
    }

    private static void makeTests(String dest, String template, String label, String[] modifiers) throws IOException {
        for (String modifier : modifiers) {
            String pack = PREFIX + "." + label + "." + (modifier.equals("") ? "plain" : modifier + "s");
            for (String type : TYPES) {
                String name = testName(type);
                String res = Spp.spp(template,
                        keys(modifier, type),
                        vars(modifier, type, pack, name));

                writeOut(dest, pack, name, res);
            }
        }
    }

    private static String upcaseFirst(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static Map<String, String> vars(String modifier, String type, String pack, String name) {
        Map<String, String> map = new HashMap<>();
        map.put("type", type);
        map.put("TYPE", type.toUpperCase());
        map.put("Type", upcaseFirst(type));
        map.put("name", name);
        map.put("default", Values.DEFAULTS.get(type));
        map.put("defaultLiteral", Values.DEFAULTS_LITERAL.get(type));
        map.put("set", Values.VALUES.get(type));
        map.put("setLiteral", Values.VALUES_LITERAL.get(type));
        map.put("modifier", (modifier.equals("") ? "" : modifier + " "));
        map.put("package", pack);
        return map;
    }

    private static Set<String> keys(String modifier, String type) {
        Set<String> set = new HashSet<>();
        set.add(type);
        if (alwaysAtomic(modifier, type)) {
            set.add("alwaysAtomic");
        }
        set.add(modifier);
        return set;
    }

    private static boolean alwaysAtomic(String modifier, String type) {
        return modifier.equals("volatile") || !(type.equals("double") || type.equals("long"));
    }

    private static String testName(String type) {
        return upcaseFirst(type) + "Test";
    }

    private static void writeOut(String destination, String pkg, String name, String contents) throws IOException {
        Path dir = Paths.get(destination, pkg.replaceAll("\\.", File.separator));
        Path file = Paths.get(destination, pkg.replaceAll("\\.", File.separator), name + ".java");
        Files.createDirectories(dir);

        boolean doWrite = true;
        try {
            List<String> l = Files.readAllLines(file);
            String exists = l.stream().collect(Collectors.joining(System.lineSeparator()));
            if (contents.equals(exists)) {
                doWrite = false;
            }
        } catch (IOException e) {
            // Moving on...
        }

        if (doWrite) {
            System.out.println("Generating: " + file);
            Files.write(file, Arrays.asList(contents), Charset.defaultCharset());
        } else {
            System.out.println("Skip, no modifications: " + file);
        }
    }

    private static String readFromResource(String name) throws IOException {
        InputStream is = Chapter0aTestGenerator.class.getResourceAsStream(name);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        StringBuilder sb = new StringBuilder();
        String l;
        while ((l = reader.readLine()) != null) {
            sb.append(l);
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

}
