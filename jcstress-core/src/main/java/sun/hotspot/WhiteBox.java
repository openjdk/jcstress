/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */

package sun.hotspot;

import java.lang.reflect.Executable;

public class WhiteBox {

    public WhiteBox() {
    }

    public static native void registerNatives();

    public native void deoptimizeAll();

    private static boolean AVAILABLE_deoptimizeMethod0 = true;

    public int deoptimizeMethod(Executable method) {
        if (AVAILABLE_deoptimizeMethod0) {
            try {
                return deoptimizeMethod0(method, false);
            } catch (Error e) {
                AVAILABLE_deoptimizeMethod0 = false;
            }
        }
        return deoptimizeMethod(method, false);
    }

    private native int deoptimizeMethod(Executable method, boolean isOsr);
    private native int deoptimizeMethod0(Executable method, boolean isOsr);

    private static boolean AVAILABLE_isClassAlive0 = true;

    public boolean isClassAlive(String name) {
        String className = name.replace('.', '/');
        if (AVAILABLE_isClassAlive0) {
            try {
                return isClassAlive0(className);
            } catch (Error e) {
                AVAILABLE_isClassAlive0 = false;
            }
        }
        return countAliveClasses0(className) > 0;
    }

    private native boolean isClassAlive0(String name);
    private native int countAliveClasses0(String name);

}