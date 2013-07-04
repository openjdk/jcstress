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

import org.openjdk.jcstress.tests.Actor2_Test;

import java.util.concurrent.CompletableFuture;

public class GetConflictTests {

    public static final Throwable ERROR1 = new MyThrowable();
    public static final Throwable ERROR2 = new MyThrowable();

    public static abstract class BaseTest implements Actor2_Test<CompletableFuture<Integer>, int[]> {
        @Override public abstract void actor1(CompletableFuture<Integer> future, int[] result);
        @Override public abstract void actor2(CompletableFuture<Integer> future, int[] result);
        @Override public CompletableFuture<Integer> newState() { return new CompletableFuture<Integer>(); }
        @Override public int[] newResult() { return new int[2]; }
    }

    public static class Complete_Complete_GetNow extends BaseTest {
        @Override
        public void actor1(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.complete(42) ? 1 : 0;
            result[1] = future.getNow(-1);
        }

        @Override
        public void actor2(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.complete(43) ? 1 : 0;
            result[1] = future.getNow(-1);
        }
    }

    public static class Complete_Complete_Get extends BaseTest {
        @Override
        public void actor1(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.complete(42) ? 1 : 0;
            result[1] = future.get();
        }

        @Override
        public void actor2(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.complete(43) ? 1 : 0;
            result[1] = future.get();
        }
    }

    public static class Complete_Force_Get extends BaseTest {
        @Override
        public void actor1(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.complete(42) ? 1 : 0;
            result[1] = future.get();
        }

        @Override
        public void actor2(CompletableFuture<Integer> future, int[] result) {
            future.force(43);
            result[0] = 1;
            result[1] = future.get();
        }
    }

    public static class Complete_Force_GetNow extends BaseTest {
        @Override
        public void actor1(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.complete(42) ? 1 : 0;
            result[1] = future.getNow(-1);
        }

        @Override
        public void actor2(CompletableFuture<Integer> future, int[] result) {
            future.force(43);
            result[0] = 1;
            result[1] = future.getNow(-1);
        }
    }

    public static class Force_Force_Get extends BaseTest {
        @Override
        public void actor1(CompletableFuture<Integer> future, int[] result) {
            future.force(42);
            result[0] = 1;
            result[1] = future.get();
        }

        @Override
        public void actor2(CompletableFuture<Integer> future, int[] result) {
            future.force(43);
            result[0] = 1;
            result[1] = future.get();
        }
    }

    public static class Force_Force_GetNow extends BaseTest {
        @Override
        public void actor1(CompletableFuture<Integer> future, int[] result) {
            future.force(42);
            result[0] = 1;
            result[1] = future.getNow(-1);
        }

        @Override
        public void actor2(CompletableFuture<Integer> future, int[] result) {
            future.force(43);
            result[0] = 1;
            result[1] = future.getNow(-1);
        }
    }

    public static class CompleteExcept_CompleteExcept_GetNow extends BaseTest {
        @Override
        public void actor1(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.completeExceptionally(ERROR1) ? 1 : 0;
            try {
                result[1] = future.getNow(-10);
            } catch (Throwable e) {
                     if (e.getCause() == ERROR1) { result[1] = -1; }
                else if (e.getCause() == ERROR2) { result[1] = -2; }
                else                  { result[1] = -100; }
            }
        }

        @Override
        public void actor2(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.completeExceptionally(ERROR2) ? 1 : 0;
            try {
                result[1] = future.getNow(-10);
            } catch (Throwable e) {
                if (e.getCause() == ERROR1) { result[1] = -1; }
                else if (e.getCause() == ERROR2) { result[1] = -2; }
                else                  { result[1] = -100; }
            }
        }
    }

    public static class CompleteExcept_CompleteExcept_Get extends BaseTest {
        @Override
        public void actor1(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.completeExceptionally(ERROR1) ? 1 : 0;
            try {
                result[1] = future.get();
            } catch (Throwable e) {
                     if (e.getCause() == ERROR1) { result[1] = -1; }
                else if (e.getCause() == ERROR2) { result[1] = -2; }
                else                  { result[1] = -100; }
            }
        }

        @Override
        public void actor2(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.completeExceptionally(ERROR2) ? 1 : 0;
            try {
                result[1] = future.get();
            } catch (Throwable e) {
                     if (e.getCause() == ERROR1) { result[1] = -1; }
                else if (e.getCause() == ERROR2) { result[1] = -2; }
                else                  { result[1] = -100; }
            }
        }
    }

    public static class Complete_CompleteExcept_Get extends BaseTest {
        @Override
        public void actor1(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.complete(42) ? 1 : 0;
            try {
                result[1] = future.get();
            } catch (Throwable e) {
                     if (e.getCause() == ERROR1) { result[1] = -1; }
                else if (e.getCause() == ERROR2) { result[1] = -2; }
                else                  { result[1] = -100; }
            }
        }

        @Override
        public void actor2(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.completeExceptionally(ERROR2) ? 1 : 0;
            try {
                result[1] = future.get();
            } catch (Throwable e) {
                     if (e.getCause() == ERROR1) { result[1] = -1; }
                else if (e.getCause() == ERROR2) { result[1] = -2; }
                else                  { result[1] = -100; }
            }
        }
    }

    public static class Complete_CompleteExcept_GetNow extends BaseTest {
        @Override
        public void actor1(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.complete(42) ? 1 : 0;
            try {
                result[1] = future.getNow(-10);
            } catch (Throwable e) {
                     if (e.getCause() == ERROR1) { result[1] = -1; }
                else if (e.getCause() == ERROR2) { result[1] = -2; }
                else                  { result[1] = -100; }
            }
        }

        @Override
        public void actor2(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.completeExceptionally(ERROR2) ? 1 : 0;
            try {
                result[1] = future.getNow(-10);
            } catch (Throwable e) {
                if (e.getCause() == ERROR1) { result[1] = -1; }
                else if (e.getCause() == ERROR2) { result[1] = -2; }
                else                  { result[1] = -100; }
            }
        }
    }

    public static class CompleteExcept_Force_Get extends BaseTest {
        @Override
        public void actor1(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.completeExceptionally(ERROR1) ? 1 : 0;
            try {
                result[1] = future.get();
            } catch (Throwable e) {
                     if (e.getCause() == ERROR1) { result[1] = -1; }
                else if (e.getCause() == ERROR2) { result[1] = -2; }
                else                  { result[1] = -100; }
            }
        }

        @Override
        public void actor2(CompletableFuture<Integer> future, int[] result) {
            future.force(43);
            result[0] = 1;
            try {
                result[1] = future.get();
            } catch (Throwable e) {
                if (e.getCause() == ERROR1) { result[1] = -1; }
                else if (e.getCause() == ERROR2) { result[1] = -2; }
                else                  { result[1] = -100; }
            }
        }
    }

    public static class CompleteExcept_Force_GetNow extends BaseTest {
        @Override
        public void actor1(CompletableFuture<Integer> future, int[] result) {
            result[0] = future.completeExceptionally(ERROR1) ? 1 : 0;
            try {
                result[1] = future.getNow(-10);
            } catch (Throwable e) {
                     if (e.getCause() == ERROR1) { result[1] = -1; }
                else if (e.getCause() == ERROR2) { result[1] = -2; }
                else                  { result[1] = -100; }
            }
        }

        @Override
        public void actor2(CompletableFuture<Integer> future, int[] result) {
            future.force(43);
            result[0] = 1;
            try {
                result[1] = future.getNow(-10);
            } catch (Throwable e) {
                     if (e.getCause() == ERROR1) { result[1] = -1; }
                else if (e.getCause() == ERROR2) { result[1] = -2; }
                else                  { result[1] = -100; }
            }
        }
    }

}
