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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Highly-specialized multiset for arrays.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class CharArrayMultiSet implements Counter<char[]> {

    private final Map<Wrapper, MutableLong> map = new HashMap<Wrapper, MutableLong>();

    @Override
    public void record(char[] result) {
        record(result, 1);
    }

    @Override
    public void record(char[] result, long count) {
        Wrapper wrapper = new Wrapper(result);
        if (!map.containsKey(wrapper)) {
            // new value, copy so that we record the key
            Wrapper copy = wrapper.copy();
            map.put(copy, new MutableLong(count));
        } else {
            // old value, hope we will reuse the existing key
            map.get(wrapper).v += count;
        }
    }

    @Override
    public Collection<char[]> elementSet() {
        Collection<char[]> coll = new ArrayList<char[]>();
        for (Wrapper w : map.keySet()) {
            coll.add(w.result);
        }
        return coll;
    }

    @Override
    public Counter<char[]> merge(Counter<char[]> other) {
        CharArrayMultiSet set = new CharArrayMultiSet();
        for (char[] b : other.elementSet()) {
            set.record(b, other.count(b));
        }
        for (char[] b : this.elementSet()) {
            set.record(b, this.count(b));
        }
        return set;
    }

    @Override
    public long count(char[] k) {
        return map.get(new Wrapper(k)).v;
    }

    public static class Wrapper {
        private final char[] result;

        public Wrapper(char[] result) {
            this.result = result;
        }

        public Wrapper copy() {
            return new Wrapper(Arrays.copyOf(result, result.length));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Wrapper wrapper = (Wrapper) o;

            if (!Arrays.equals(result, wrapper.result)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(result);
        }
    }

}
