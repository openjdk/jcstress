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
package org.openjdk.jcstress.tests;

/**
 * Termination test.
 *
 * This is useful to test if the actor terminates upon receiving the particular signal from other thread.
 *
 * @param <S>
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public interface TerminationTest<S> extends ConcurrencyTest {

    /**
     * Body for actor thread.
     *
     * This method is called once per state, and supposed to be blocked inside the body
     * until signal is received.
     *
     * @param s state to work on
     * @throws Exception
     */
    void actor1(S s) throws Exception;

    /**
     * Body for signal.
     *
     * This method is called once per state, and some time after the actor thread started to execute.
     *
     * @param s state to work on
     * @param actor1 actor thread
     * @throws Exception
     */
    void signal(S s, Thread actor1) throws Exception;

    /**
     * Create new object to work on.
     *
     * Conventions:
     *   - this method is called only within the exclusive thread
     *   - this method should return new object at every call; answering cached object will interfere with test correctness
     *   - there are safe publication guarantees enforced by the Runner
     *       (i.e. for any given s, (newState(s) hb actor1(s)) and (newState(s) hb signal(s))
     *
     * @return fresh state object
     */
    S newState();

}
