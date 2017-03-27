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
package org.openjdk.jcstress.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StringUtils {

    public static String cutoff(String src, int len) {
        src = src.replaceAll("\u0000", " ");
        while (src.contains("  ")) {
            src = src.replaceAll("  ", " ");
        }
        String trim = src.replaceAll("\n", "").trim();
        if (trim.length() <= len) {
            return trim;
        }
        int min = Math.min(len - 3, trim.length());
        String substring = trim.substring(0, min);
        if (!substring.equals(trim)) {
            return substring + "...";
        } else {
            return substring;
        }
    }

    public static Collection<String> splitQuotedEscape(String src) {
        List<String> results = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (char ch : src.toCharArray()) {
            if (ch == ' ' && !escaped) {
                String s = sb.toString();
                if (!s.isEmpty()) {
                    results.add(s);
                    sb = new StringBuilder();
                }
            } else if (ch == '\"') {
                escaped ^= true;
            } else {
                sb.append(ch);
            }
        }

        String s = sb.toString();
        if (!s.isEmpty()) {
            results.add(s);
        }

        return results;
    }

    public static String chunkName(String name) {
        return name.replace("org.openjdk.jcstress.tests", "o.o.j.t");
    }

    public static String upcaseFirst(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static boolean hasText(String s) {
        return (s != null) && !s.isEmpty();
    }

    public static String join(List<String> list, String delim) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : list) {
            if (first) {
                first = false;
            } else {
                sb.append(delim);
            }
            sb.append(s);
        }
        return sb.toString();
    }

    public static String getStacktrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    public static String getFirstLine(String src) {
        int endLine = src.indexOf("\n");
        if (endLine > 0) {
            return src.substring(0, endLine).trim();
        }
        return src;
    }
}
