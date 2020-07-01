/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class CPULayout {
    private final int maxThreads;
    private final BitSet availableCPUs;

    public CPULayout(int maxThreads) {
        availableCPUs = new BitSet(maxThreads);
        this.maxThreads = maxThreads;
        availableCPUs.set(0, maxThreads);
    }

    public synchronized List<Integer> tryAcquire(int requested) {
        // Make fat tasks bypass in exclusive mode:
        final int threads = Math.min(requested, maxThreads);

        if (availableCPUs.cardinality() < threads) {
            return null;
        }

        List<Integer> claimed = new ArrayList<>();
        for (int c = 0; c < maxThreads; c++) {
            if (availableCPUs.get(c)) {
                availableCPUs.set(c, false);
                claimed.add(c);
                if (claimed.size() == threads) {
                    return claimed;
                }
            }
        }

        throw new IllegalStateException("Cannot happen");
    }

    public synchronized void release(List<Integer> requested) {
        for (int c : requested) {
            availableCPUs.set(c, true);
        }
    }

}
