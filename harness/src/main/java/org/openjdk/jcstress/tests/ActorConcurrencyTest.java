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
 * Actor concurrency tests accept one or more:
 *   - actors:     threads actively mutating the state, and recording the current state
 *   - arbiters:   threads arbitrating the results *after* actors finish
 *
 * Shared state is represented by state object. Runners will ensure enough fresh state objects would
 * be provided to the tests methods to unfold even the finest races.
 *
 * Conventions for actors:
 *   - the method is called only by single actor thread, once per state
 *   - for any given state, the order vs another actors is deliberately unspecified
 *   - any given state will be eventually visited by all actors
 *   - actor can store the observed state in the result array
 *   - actor should not rely on the default values in the result array, and should set all elements on every call
 *   - actor can not store the reference to result array
 *
 * Conventions for arbiters:
 *   - everything applicable to actors, EXCEPT:
 *   - for any given state, arbiter visits the state the last, with all actor memory effects visible
 *
 * @param <S> state object type
 * @param <R> result object type
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public interface ActorConcurrencyTest<S, R> extends ConcurrencyTest {

    /**
     * Produce new state holder.
     * Harness can reuse holders as appropriate.
     * @return state object
     */
    S newState();

    /**
     * Produce the new result holder.
     * Harness can reuse holders as appropriate.
     *
     * @return result holder.
     */
    R newResult();
}
