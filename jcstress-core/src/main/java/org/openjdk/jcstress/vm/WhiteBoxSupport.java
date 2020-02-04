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
import java.util.List;

public class WhiteBoxSupport {

    private static WhiteBox whiteBox;
    private static volatile boolean tried;
    private static volatile Collection<String> methods;

    private static volatile boolean AVAILABLE_ALL;
    private static volatile boolean AVAILABLE_METHOD;

    private static volatile Throwable EXCEPTION_ALL;
    private static volatile Throwable EXCEPTION_METHOD;

    public static Throwable errorAll() {
        return EXCEPTION_ALL;
    }

    public static Throwable errorMethod() {
        return EXCEPTION_METHOD;
    }

    public static void init() {
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

        try {
            w.deoptimizeMethod(WhiteBoxSupport.class.getMethod("initSafely"));
            w.isClassAlive(WhiteBoxSupport.class.getName());
            AVAILABLE_METHOD = true;
        } catch (Throwable ex) {
            EXCEPTION_METHOD = ex;
            AVAILABLE_METHOD = false;
        }

        try {
            w.deoptimizeAll();
            AVAILABLE_ALL = true;
        } catch (Throwable ex) {
            EXCEPTION_ALL = ex;
            AVAILABLE_ALL = false;
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

    public static void tryDeopt(DeoptMode mode) {
        WhiteBox w = whiteBox;
        if (w != null) {
            switch (mode) {
                case ALL:
                    if (AVAILABLE_ALL) {
                        w.deoptimizeAll();
                    }
                    break;
                case METHOD:
                    if (AVAILABLE_METHOD) {
                        try {
                            for (Method m : getJCStressMethods()) {
                                w.deoptimizeMethod(m);
                            }
                        } catch (IOException e) {
                            throw new IllegalStateException();
                        }
                    }
                    break;
                case NONE:
                    break;
                default:
                    throw new IllegalStateException("Unknown deopt mode: " + mode);
            }
        }
    }

    private static Collection<Method> getJCStressMethods() throws IOException {
        Collection<String> ms = methods;
        if (ms == null) {
            ms = Reflections.getClassNames("org.openjdk.jcstress");
            methods = ms;
        }

        List<Method> im = new ArrayList<>();
        for (String name : ms) {
            // Avoid loading classes for tests that were not yet executed.
            if (!whiteBox.isClassAlive(name)) continue;
            try {
                Collections.addAll(im, Class.forName(name).getDeclaredMethods());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException();
            }
        }
        return im;
    }

}
