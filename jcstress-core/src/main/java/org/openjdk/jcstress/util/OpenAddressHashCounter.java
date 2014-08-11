/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OpenAddressHashCounter<R> implements Counter<R>, Serializable {

    /*
     * Implementation notes: the requirements for Counter are very relaxed,
     * and therefore this implementation cuts around the edges heavily:
     *   - we do not allow nulls!
     *   - we do not support removals, therefore cleanup strategy is not
     *     required to implement;
     *   - values are always longs, saving us additional dereferences and
     *     boxing/unboxing;
     *   - resizes are infrequent, since most of the time the case count is
     *     minuscule;
     */

    static final int MAX_TRIES = 3;

    Object[] keys;
    long[] counts;
    int length;

    public OpenAddressHashCounter() {
        this(16);
    }

    public OpenAddressHashCounter(int len) {
        length = len;

        @SuppressWarnings("unchecked")
        R[] table = (R[]) new Object[len];
        this.keys = table;
        this.counts = new long[len];
    }

    @Override
    public void record(R result) {
        recordWithTries(result, 1, MAX_TRIES);
    }

    @Override
    public void record(R result, long count) {
        recordWithTries(result, count, MAX_TRIES);
    }

    private void recordWithTries(R result, long count, int maxTries) {
        int idx = result.hashCode() & (length - 1);

        int tryCount = 0;
        Object k = keys[idx];
        while (k != null) {

            // hit the bucket, update and exit
            if (k.equals(result)) {
                counts[idx] += count;
                return;
            }

            // trying the next bucket
            idx = (idx + 1) & (length - 1);
            k = keys[idx];

            // whoops, map is overloaded, resize to make up
            // the space, try again (succeeding), and exit;
            // we might want to resize early
            if (tryCount++ > maxTries) {
                resize();
                recordWithTries(result, count, Integer.MAX_VALUE);
                return;
            }
        }

        // completely new key, insert, and exit
        keys[idx] = decouple(result);
        counts[idx] = count;
    }

    private void resize() {
        Object[] prevKeys = keys;
        long[] prevCounts = counts;

        // double so:
        int newLen = (length << 1);

        // allocate new stuff
        @SuppressWarnings("unchecked")
        R[] table = (R[]) new Object[newLen];
        keys = table;
        counts = new long[newLen];
        length = newLen;

        // rehash the entire map
        for (int kIdx = 0; kIdx < prevKeys.length; kIdx++) {
            Object k = prevKeys[kIdx];
            if (k != null) {
                int idx = k.hashCode() & (length - 1);
                while (keys[idx] != null) {
                    idx = (idx + 1) & (length - 1);
                }
                keys[idx] = k;
                counts[idx] = prevCounts[kIdx];
            }
        }
    }

    @Override
    public long count(R result) {
        int idx = result.hashCode() & ((length - 1));
        while (keys[idx] != null) {
            if (keys[idx].equals(result)) {
                return counts[idx];
            }
            idx = (idx + 1) & (length - 1);
        }
        return 0L;
    }

    private static <T> T decouple(T result) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(result);
            oos.close();

            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);

            @SuppressWarnings("unchecked")
            T t = (T)ois.readObject();

            return t;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Collection<R> elementSet() {
        List<R> res = new ArrayList<>();
        for (Object k : keys) {
            if (k != null) {
                @SuppressWarnings("unchecked")
                R e = (R) k;
                res.add(e);
            }
        }
        return res;
    }

}
