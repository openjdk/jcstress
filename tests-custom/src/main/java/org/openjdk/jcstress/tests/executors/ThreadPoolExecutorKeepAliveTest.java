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
package org.openjdk.jcstress.tests.executors;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.LongResult2;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@JCStressTest
@Description("Tests if ThreadPoolExecutor invariant can be violated: keepAliveTime == 0 &#8658; !allowCoreThreadTimeOut")
@Outcome(id = "[1, 1]", expect = Expect.ACCEPTABLE, desc = "allowCoreThreadTimeOut had changed, and keepAliveTime failed to change.")
@Outcome(id = "[0, 0]", expect = Expect.ACCEPTABLE, desc = "keepAliveTime had changed, and allowCoreThreadTimeOut failed to change.")
@Outcome(id = "[1, 0]", expect = Expect.ACCEPTABLE_INTERESTING, desc = "The update under race can break the (keepAliveTime == 0 &#8658; !allowCoreThreadTimeOut) invariant.")
@State
public class ThreadPoolExecutorKeepAliveTest {

    public final ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 2, 1L,
            TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(2));

    @Actor
    public void actor1() {
        try {
            pool.allowCoreThreadTimeOut(true);
        } catch (IllegalArgumentException ignored) {}
    }

    @Actor
    public void actor2() {
        try {
            pool.setKeepAliveTime(0, TimeUnit.MILLISECONDS);
        } catch (IllegalArgumentException ignored) {}
    }

    @Arbiter
    public void actor3(LongResult2 r) {
        r.r1 = pool.allowsCoreThreadTimeOut() ? 1 : 0;
        r.r2 = pool.getKeepAliveTime(TimeUnit.MILLISECONDS);
    }
}
