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
package org.openjdk.jcstress.vm;

import org.openjdk.jcstress.util.Reflections;
import sun.hotspot.WhiteBox;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class WhiteBoxSupport {

    private static WhiteBox whiteBox;
    private static volatile boolean tried;
    private static volatile Mode mode;
    private static volatile Collection<Method> infraMethods;

    enum Mode {
        DEOPT_ALL,
        DEOPT_METHOD,
    }

    public static void init() throws Throwable {
        if (tried) return;
        try {
            initAndTest();
        } finally {
            tried = true;
        }
    }

    private static void initAndTest() {
        WhiteBox.registerNatives();
        WhiteBox w = new WhiteBox();

        Throwable deoptMethod = null;
        try {
            w.deoptimizeMethod(WhiteBoxSupport.class.getMethod("initSafely"));
            w.isClassAlive(WhiteBoxSupport.class.getName());
        } catch (Throwable ex) {
            deoptMethod = ex;
        }

        Throwable deoptAll = null;
        try {
            w.deoptimizeAll();
        } catch (Throwable ex) {
            deoptAll = ex;
        }

        if (deoptMethod == null) {
            mode = Mode.DEOPT_METHOD;
        } else if (deoptAll == null) {
            mode = Mode.DEOPT_ALL;
        } else {
            IllegalStateException whiteBoxFailed = new IllegalStateException();
            whiteBoxFailed.addSuppressed(deoptAll);
            whiteBoxFailed.addSuppressed(deoptMethod);
            throw whiteBoxFailed;
        }
        whiteBox = w;
    }

    public static void initSafely() {
        if (tried) return;
        try {
            initAndTest();
        } catch (Throwable e) {
            // expected
        } finally {
            tried = true;
        }
    }

    public static void tryDeopt(int actionProbRatio) {
        WhiteBox w = whiteBox;
        if (w != null) {
            if (ThreadLocalRandom.current().nextInt(actionProbRatio) != 0)
                return;

            switch (mode) {
                case DEOPT_ALL:
                    w.deoptimizeAll();
                    break;
                case DEOPT_METHOD:
                    try {
                        for (Method m : getJCStressMethods()) {
                            w.deoptimizeMethod(m);
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException();
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown deopt mode: " + mode);
            }
        }
    }

    private static Collection<Method> getJCStressMethods() throws IOException {
        Collection<Method> im = infraMethods;
        if (im == null) {
            im = new ArrayList<>();
            Collection<String> names = new ArrayList<>();
            names.addAll(Reflections.getClassNames("org.openjdk.jcstress"));
            for (String name : names) {
                // Avoid loading classes
                if (!whiteBox.isClassAlive(name)) continue;
                try {
                    Class<?> aClass = Class.forName(name);
                    Collections.addAll(im, aClass.getDeclaredMethods());
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException();
                }
            }
            infraMethods = im;
        } return im;
    }

}
