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
package org.openjdk.jcstress.tests.future.completable;

import org.openjdk.jcstress.tests.Actor1_Effector1_Test;

import java.util.concurrent.CompletableFuture;

public class GetRaceTests {

    public static abstract class BaseTest implements Actor1_Effector1_Test<CompletableFuture<Integer>, int[]> {
        @Override public abstract void actor1(CompletableFuture<Integer> future);
        @Override public abstract void observe(CompletableFuture<Integer> future, int[] result);
        @Override public CompletableFuture<Integer> newState() { return new CompletableFuture<Integer>(); }
        @Override public int[] newResult() { return new int[1]; }
    }

    public static class Force_Get extends BaseTest {
        @Override
        public void actor1(CompletableFuture<Integer> future) {
            future.force(42);
        }

        @Override
        public void actor1(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.get();
        }
    }

    public static class Force_GetNow extends BaseTest {
        @Override
        public void actor1(CompletableFuture<Integer> future) {
            future.force(42);
        }

        @Override
        public void actor1(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.getNow(-1);
        }
    }

    public static class Complete_Get extends BaseTest {
        @Override
        public void actor1(CompletableFuture<Integer> future) {
            future.complete(42);
        }

        @Override
        public void actor1(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.get();
        }
    }

    public static class Complete_GetNow extends BaseTest {
        @Override
        public void actor1(CompletableFuture<Integer> future) {
            future.complete(42);
        }

        @Override
        public void actor1(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.getNow(-1);
        }
    }

    public static final Throwable ERROR = new MyThrowable();

    public static class CompleteExcept_Get extends BaseTest {
        @Override
        public void actor1(CompletableFuture<Integer> future) {
            future.completeExceptionally(ERROR);
        }

        @Override
        public void actor1(CompletableFuture<Integer> future, int[] result) {
            try {
                result[0] = future.get();
            } catch (Throwable e) {
                if (e.getCause() == ERROR) {
                    result[0] = -1;
                } else {
                    result[0] = -100;
                }
            }
        }
    }

    public static class CompleteExcept_GetNow extends BaseTest {
        @Override
        public void actor1(CompletableFuture<Integer> future) {
            future.completeExceptionally(ERROR);
        }

        @Override
        public void actor1(CompletableFuture<Integer> future, int[] result) {
            try {
                result[0] = future.getNow(-10);
            } catch (Throwable e) {
                if (e.getCause() == ERROR) {
                    result[0] = -1;
                } else {
                    result[0] = -100;
                }
            }
        }
    }


}
