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
package org.openjdk.jcstress.tests.interrupt;

import org.openjdk.jcstress.tests.TerminationTest;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

public class WeakReferenceTest implements TerminationTest<WeakReferenceTest.State> {

    @Override
    public void actor1(State s) {
        while (s.ref.get() != null) {
            // burn!
        }
    }

    @Override
    public void signal(State s, Thread actor1) throws InterruptedException {
        s.referent = null;

        // should eventually complete, not testing here
        while (s.refQueue.poll() != s.ref) {
            System.gc();
            TimeUnit.MILLISECONDS.sleep(10);
        }
    }

    @Override
    public State newState() {
        return new State();
    }

    public static class State {
        public volatile Object referent;
        public final WeakReference<Object> ref;
        public final ReferenceQueue<Object> refQueue;

        public State() {
            referent = new Object();
            refQueue = new ReferenceQueue<Object>();
            ref = new WeakReference<Object>(referent, refQueue);
        }
    }

}
