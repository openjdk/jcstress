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
package org.openjdk.jcstress.infra.results;

import org.openjdk.jcstress.annotations.Result;
import sun.misc.Contended;

import java.io.Serializable;

@Result
public class ByteResult8 implements Serializable {

    @Contended
    public byte r1;

    @Contended
    public byte r2;

    @Contended
    public byte r3;

    @Contended
    public byte r4;

    @Contended
    public byte r5;

    @Contended
    public byte r6;

    @Contended
    public byte r7;

    @Contended
    public byte r8;

    @Override
    public String toString() {
        return "[" + r1 + ", " + r2 + ", " + r3 + ", " + r4 + ", " + r5 + ", " + r6 + ", " + r7 + ", " + r8 + ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ByteResult8 that = (ByteResult8) o;

        if (r1 != that.r1) return false;
        if (r2 != that.r2) return false;
        if (r3 != that.r3) return false;
        if (r4 != that.r4) return false;
        if (r5 != that.r5) return false;
        if (r6 != that.r6) return false;
        if (r7 != that.r7) return false;
        if (r8 != that.r8) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) r1;
        result = 31 * result + (int) r2;
        result = 31 * result + (int) r3;
        result = 31 * result + (int) r4;
        result = 31 * result + (int) r5;
        result = 31 * result + (int) r6;
        result = 31 * result + (int) r7;
        result = 31 * result + (int) r8;
        return result;
    }

}
