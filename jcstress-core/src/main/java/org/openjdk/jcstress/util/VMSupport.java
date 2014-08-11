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
package org.openjdk.jcstress.util;

import sun.hotspot.WhiteBox;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class VMSupport {

    private static WhiteBox whiteBox;
    private static volatile boolean inited;
    private static volatile Collection<Method> infraMethods;

    public static boolean tryInit() {
        if (inited) return true;
        try {
            WhiteBox w = WhiteBox.getWhiteBox();
            w.deoptimizeAll();
            whiteBox = w;
            return true;
        } catch (UnsatisfiedLinkError e) {
            // expected
            return false;
        } finally {
            inited = true;
        }
    }

    public static void tryDeoptimizeAllInfra(int actionProbRatio) {
        WhiteBox w = whiteBox;
        if (w != null) {
            if (ThreadLocalRandom.current().nextInt(actionProbRatio) != 0)
                return;

            try {
                Collection<Method> im = infraMethods;
                if (im == null) {
                    im = new ArrayList<>();
                    Collection<String> infraNames = new ArrayList<>();
                    infraNames.addAll(Reflections.getClassNames("org.openjdk.jcstress.infra"));
                    infraNames.addAll(Reflections.getClassNames("org.openjdk.jcstress.util"));
                    for (String name : infraNames) {
                        try {
                            Class<?> aClass = Class.forName(name);
                            Collections.addAll(im, aClass.getDeclaredMethods());
                        } catch (ClassNotFoundException e) {
                            throw new IllegalStateException();
                        }
                    }
                    infraMethods = im;
                }

                for (Method m : im) {
                    w.deoptimizeMethod(m);
                }
            } catch (IOException e) {
                throw new IllegalStateException();
            }
        }
    }

}
