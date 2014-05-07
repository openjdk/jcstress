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
public class DoubleResult4 implements Serializable {

    @Contended
    public double r1;

    @Contended
    public double r2;

    @Contended
    public double r3;

    @Contended
    public double r4;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DoubleResult4 that = (DoubleResult4) o;

        if (Double.compare(that.r1, r1) != 0) return false;
        if (Double.compare(that.r2, r2) != 0) return false;
        if (Double.compare(that.r3, r3) != 0) return false;
        if (Double.compare(that.r4, r4) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = r1 != +0.0d ? Double.doubleToLongBits(r1) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = r2 != +0.0d ? Double.doubleToLongBits(r2) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = r3 != +0.0d ? Double.doubleToLongBits(r3) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = r4 != +0.0d ? Double.doubleToLongBits(r4) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "[" + r1 + ", " + r2 + ", " + r3 + ", " + r4 + ']';
    }

}
