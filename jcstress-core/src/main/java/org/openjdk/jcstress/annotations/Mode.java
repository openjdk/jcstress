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
package org.openjdk.jcstress.annotations;

/**
 * JCStress test mode.
 */
public enum Mode {

    /**
     * Continuous mode: run several {@link Actor}, {@link Arbiter} threads, and
     * collect the histogram of {@link Result}s.
     *
     * <p>In this mode, each {@link Actor} and {@link Arbiter} method is called
     * exactly once per {@link State} instance.
     *
     * <p>The test runs continuously until the test time expires.
     */
    Continuous,

    /**
     * Termination mode: run a single {@link Actor} with a blocking/looping operation,
     * and see if it responds to a {@link Signal}.
     *
     * <p>In this mode, each {@link Actor} and {@link Signal} method is called
     * exactly once per {@link State} instance.
     *
     * <p>The test finishes as soon as {@link Actor} exits, possibly as the reaction
     * to a {@link Signal}.
     */
    Termination,

    /**
     * Deadlock mode: run several {@link Actor} threads continuously, and see if
     * any actor deadlocks.
     *
     * <p>In contrast to other modes, each {@link Actor} methods is called over the
     * same {@link State} instance, the same number of times. This allows testing
     * deadlock conditions more precisely, while matching the test symmetry exactly.
     *
     * <p>If you need a test that still runs {@link Actor} once per
     * {@link State}, consider using {@link #Continuous} mode, producing
     * only {@link Expect#ACCEPTABLE} results.
     *
     * <p>The test runs continuously until the test time expires.
     */
    Deadlock,

}
