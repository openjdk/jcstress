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

import java.io.Serializable;

@Result
public class StringResult4 implements Serializable {

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    public String r1;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    public String r2;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    public String r3;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    public String r4;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StringResult4 that = (StringResult4) o;

        if (r1 != null ? !r1.equals(that.r1) : that.r1 != null) return false;
        if (r2 != null ? !r2.equals(that.r2) : that.r2 != null) return false;
        if (r3 != null ? !r3.equals(that.r3) : that.r3 != null) return false;
        return r4 != null ? r4.equals(that.r4) : that.r4 == null;

    }

    @Override
    public int hashCode() {
        int result = r1 != null ? r1.hashCode() : 0;
        result = 31 * result + (r2 != null ? r2.hashCode() : 0);
        result = 31 * result + (r3 != null ? r3.hashCode() : 0);
        result = 31 * result + (r4 != null ? r4.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "[" + r1 + ", " + r2 + ", " + r3 + ", " + r4 + ']';
    }
}
