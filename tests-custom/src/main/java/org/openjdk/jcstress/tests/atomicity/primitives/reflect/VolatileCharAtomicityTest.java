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
package org.openjdk.jcstress.tests.atomicity.primitives.reflect;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.CharResult1;
import org.openjdk.jcstress.tests.atomicity.primitives.Constants;

import java.lang.reflect.Field;

/**
 * Tests if volatile primitive doubles experience non-atomic updates.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
@JCStressTest
@Outcome(id = "[N]", expect = Expect.ACCEPTABLE, desc = "Default value for the field. Observers are allowed to see the default value for the field, because there is the data race between reader and writer.")
@Outcome(id = "[A]", expect = Expect.ACCEPTABLE, desc = "The value set by the actor thread. Observer sees the complete update.")
@State
public class VolatileCharAtomicityTest {

    private static Field FIELD;

    static {
        try {
            FIELD = VolatileCharAtomicityTest.class.getDeclaredField("x");
            FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }

    }

    volatile char x;

    @Actor
    public void actor1() {
        try {
            FIELD.setChar(this, Constants.CHAR_SAMPLE);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Actor
    public void actor2(CharResult1 r) {
        try {
            r.r1 = FIELD.getChar(this) == 0 ? 'N' : 'A';
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

}
