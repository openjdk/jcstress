/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress.tests.future;

import org.openjdk.jcstress.infra.annotations.Actor;
import org.openjdk.jcstress.infra.annotations.ConcurrencyStressTest;
import org.openjdk.jcstress.infra.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult1;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ConcurrencyStressTest
public class FutureTaskSetTest {

    @Actor
    public void actor1(MyFutureTask s) {
        s.set(42);
    }

    @Actor
    public void actor2(MyFutureTask s, IntResult1 r) {
        try {
            Integer key = s.get(1, TimeUnit.HOURS);
            r.r1 = (key == null) ? -1 : key;
        } catch (InterruptedException e) {
            r.r1 = Integer.MIN_VALUE;
        } catch (ExecutionException e) {
            r.r1 = Integer.MIN_VALUE;
        } catch (TimeoutException e) {
            r.r1 = -2;
        }
    }

    @State
    public static class MyFutureTask extends FutureTask<Integer> {
        private static final Callable<Integer> EMPTY_CALLABLE = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                throw new IllegalStateException("Should not reach here");
            }
        };

        public MyFutureTask() {
            super(EMPTY_CALLABLE);
        }

        @Override
        public void set(Integer integer) {
            super.set(integer);
        }
    }

}
