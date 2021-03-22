/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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

import java.util.List;

public class CompileMode {

    public static final int VARIANTS = 3;
    public static final int UNIFIED = -1;

    private final int mode;
    private final List<String> actorNames;
    private final int actors;

    public CompileMode(int mode, List<String> actorNames, int actors) {
        this.mode = mode;
        this.actorNames = actorNames;
        this.actors = actors;
    }

    public static int casesFor(int actors) {
        int cases = 1;
        for (int c = 0; c < actors; c++) {
            cases *= VARIANTS;
        }
        return cases;
    }

    private int actorMode(int actor) {
        int m = mode;
        for (int a = 0; a < actor; a++) {
            m /= VARIANTS;
        }
        return m % VARIANTS;
    }

    public boolean isInt(int actor) {
        return (mode != UNIFIED) && (actorMode(actor) == 0);
    }

    public boolean isC1(int actor) {
        return (mode != UNIFIED) && (actorMode(actor) == 1);
    }

    public boolean isC2(int actor) {
        return (mode != UNIFIED) && (actorMode(actor) == 2);
    }

    public boolean hasC2() {
        if (mode == UNIFIED) {
            return true;
        }
        for (int a = 0; a < actors; a++) {
            if (isC2(a)) return true;
        }
        return false;
    }

    private String actorModeToString(int actor) {
        int v = actorMode(actor);
        switch (v) {
            case 0: return "Interpreter";
            case 1: return "C1";
            case 2: return "C2";
            default:
                throw new IllegalStateException("Unhandled variant: " + v);
        }
    }

    public String toString() {
        if (mode == UNIFIED) {
            return "unified across all actors";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("split; ");
        for (int a = 0; a < actors; a++) {
            if (a != 0) {
                sb.append(", ");
            }
            sb.append("\"");
            sb.append(actorNames.get(a));
            sb.append("\"");
            sb.append(": ");
            sb.append(actorModeToString(a));
        }
        return sb.toString();
    }

}
