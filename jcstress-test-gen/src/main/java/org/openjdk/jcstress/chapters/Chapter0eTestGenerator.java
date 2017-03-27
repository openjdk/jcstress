/*
 * Copyright (c) 2017, Red Hat Inc. All rights reserved.
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
import org.openjdk.jcstress.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Chapter0eTestGenerator {

    public static final String PREFIX = "org.openjdk.jcstress.tests";
    public static final String[] TYPES = new String[]{"byte", "boolean", "char", "short", "int", "float", "long", "double", "String"};
    public static final String[] MODIFIERS = new String[]{ "", "volatile" };

    public static void main(String... args) throws IOException {
        if (args.length < 1) {
            throw new IllegalStateException("Need a destination argument");
        }
        String dest = args[0];

        makeTests(
                dest,
                GeneratorUtils.readFromResource("/acqrel/X-FieldAcqRelTest.java.template"),
                "acqrel.fields"
        );

        makeTests(
                dest,
                GeneratorUtils.readFromResource("/acqrel/X-ArrayAcqRelTest.java.template"),
                "acqrel.arrays"
        );

    }

    private static void makeTests(String dest, String template, String label) throws IOException {
        for (String modifier : MODIFIERS) {
            String pack = PREFIX + "." + label + "." + (modifier.equals("") ? "plain" : modifier + "s");
            for (String typeV : TYPES) {
                for (String typeG : TYPES) {
                    String name = testName(typeG, typeV);
                    String res = Spp.spp(template,
                            keys(modifier, typeG, label),
                            vars(modifier, typeG, typeV, pack, name));

                    GeneratorUtils.writeOut(dest, pack, name, res);
                }
            }
        }
    }


    private static Map<String, String> vars(String modifier, String typeG, String typeV, String pack, String name) {
        Map<String, String> map = new HashMap<>();
        map.put("typeG", typeG);
        map.put("typeV", typeV);
        map.put("TYPEG", typeG.toUpperCase());
        map.put("TYPEV", typeV.toUpperCase());
        map.put("TypeG", StringUtils.upcaseFirst(typeG));
        map.put("TypeV", StringUtils.upcaseFirst(typeV));
        map.put("TG", GeneratorUtils.toDescriptor(typeG));
        map.put("TV", GeneratorUtils.toDescriptor(typeV));
        map.put("name", name);
        map.put("defaultG", Values.DEFAULTS.get(typeG));
        map.put("defaultV", Values.DEFAULTS.get(typeV));
        map.put("defaultGLiteral", Values.DEFAULTS_LITERAL.get(typeG));
        map.put("defaultVLiteral", Values.DEFAULTS_LITERAL.get(typeV));
        map.put("setG", Values.VALUES.get(typeG));
        map.put("setV", Values.VALUES.get(typeV));
        map.put("setGLiteral", Values.VALUES_LITERAL.get(typeG));
        map.put("setVLiteral", Values.VALUES_LITERAL.get(typeV));
        map.put("modifier", (modifier.equals("") ? "" : modifier + " "));
        map.put("package", pack);
        return map;
    }

    private static Set<String> keys(String modifier, String type, String label) {
        Set<String> set = new HashSet<>();
        set.add(type);
        if (racy(modifier, type, label)) {
            set.add("racy");
        }
        set.add(modifier);
        return set;
    }

    private static boolean racy(String modifier, String type, String label) {
        return (!modifier.equals("volatile") || label.contains("array"));
    }

    private static String testName(String typeG, String typeV) {
        return StringUtils.upcaseFirst(typeG) + StringUtils.upcaseFirst(typeV) + "Test";
    }
}
