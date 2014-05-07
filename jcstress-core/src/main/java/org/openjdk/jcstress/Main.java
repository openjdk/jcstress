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
package org.openjdk.jcstress;

import org.openjdk.jcstress.util.ContendedSupport;
import org.openjdk.jcstress.util.VMSupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Main entry point.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("Java Concurrency Stress Tests");
        System.out.println("---------------------------------------------------------------------------------");
        printVersion(System.out);
        System.out.println();

        Options opts = new Options(args);
        if (!opts.parse()) {
            System.exit(1);
        }

        if (opts.shouldList()) {
            for (String test : org.openjdk.jcstress.JCStress.getTests(opts.getTestFilter())) {
                System.out.println(test);
            }
        } else {
            boolean vmSupportInited;
            try {
                vmSupportInited = VMSupport.tryInit();
            } catch (NoClassDefFoundError c) {
                // expected on JDK 7 and lower
                vmSupportInited = false;
            }

            if (!vmSupportInited) {
                System.out.println("Non-fatal: VM support for online deoptimization is not enabled, tests might miss some issues.\nPossible reasons are:\n" +
                        "  1) unsupported JDK, only JDK 8+ is supported; \n" +
                        "  2) -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI are missing; \n" +
                        "  3) the jcstress JAR is not added to -Xbootclasspath/a\n");
            } else {
                System.out.println("VM support is initialized.\n");
            }

            if (!ContendedSupport.tryContended()) {
                System.out.println("Non-fatal: VM support for @Contended is not enabled, tests might run slower.\nPossible reasons are:\n" +
                        "  1) unsupported JDK, only JDK 8+ is supported; \n" +
                        "  2) -XX:-RestrictContended is missing, or the jcstress JAR is not added to -Xbootclasspath/a\n");
            } else {
                System.out.println("@Contended is in use.\n");
            }

            new JCStress().run(opts);
        }
    }

    static void printVersion(PrintStream out) {
        Class clazz = Main.class;
        String className = clazz.getSimpleName() + ".class";
        String classPath = clazz.getResource(className).toString();
        if (!classPath.startsWith("jar")) {
            return;
        }
        String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
        InputStream stream = null;
        try {
            stream = new URL(manifestPath).openStream();
            Manifest manifest = new Manifest(stream);
            Attributes attr = manifest.getMainAttributes();
            out.printf("Rev: %s, built by %s with %s at %s\n",
                    attr.getValue("Implementation-Build"),
                    attr.getValue("Built-By"),
                    attr.getValue("Build-Jdk"),
                    attr.getValue("Build-Time")
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                // swallow
            }
        }
    }

}
