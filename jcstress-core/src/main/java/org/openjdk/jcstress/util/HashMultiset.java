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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HashMultiset<T> implements Multiset<T>, Serializable {

    private final Map<T, Long> map;
    private long size;

    public HashMultiset() {
        map = new HashMap<>();
    }

    @Override
    public void add(T element) {
        add(element, 1);
    }

    @Override
    public void add(T element, long add) {
        Long count = map.get(element);
        if (count == null) {
            count = 0L;
        }
        count += add;
        size += add;
        map.put(element, count);
    }

    @Override
    public long count(T element) {
        Long count = map.get(element);
        return (count == null) ? 0 : count;
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public Collection<T> keys() {
        return Collections.unmodifiableCollection(map.keySet());
    }
}
