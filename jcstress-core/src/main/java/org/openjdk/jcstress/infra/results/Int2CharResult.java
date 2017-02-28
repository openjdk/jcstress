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
package org.openjdk.jcstress.infra.results;

import org.openjdk.jcstress.annotations.Result;

import java.io.Serializable;

@Result
public class Int2CharResult implements Serializable {

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    public int r1;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    public int r2;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    public char r3;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Int2CharResult that = (Int2CharResult) o;

        if (r1 != that.r1) return false;
        if (r2 != that.r2) return false;
        return r3 == that.r3;
    }

    @Override
    public int hashCode() {
        int result = r1;
        result = 31 * result + r2;
        result = 31 * result + (int) r3;
        return result;
    }

    @Override
    public String toString() {
        return r1 + ", " + r2 + ", " + r3;
    }

}
