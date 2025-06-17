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

import org.openjdk.jcstress.annotations.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@Outcome(id = "TERMINATED", expect = Expect.ACCEPTABLE, desc = "The thread had successfully terminated.")
@Outcome(id = "STALE",      expect = Expect.FORBIDDEN,  desc = "Thread had failed to respond.")
public class VarHandleBusyLoopTest {
    static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(VarHandleBusyLoopTest.class, "isStopped", boolean.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean isStopped;

    @JCStressTest(Mode.Termination)
    @JCStressMeta(VarHandleBusyLoopTest.class)
    @State
    public static class Opaque_Opaque extends VarHandleBusyLoopTest {
        @Signal public void signal() { VH.setOpaque(this, true); }
        @Actor  public void actor1() { while (!(boolean) VH.getOpaque(this)); }
    }

    @JCStressTest(Mode.Termination)
    @JCStressMeta(VarHandleBusyLoopTest.class)
    @State
    public static class Opaque_Acquire extends VarHandleBusyLoopTest {
        @Signal public void signal() { VH.setOpaque(this, true); }
        @Actor  public void actor1() { while (!(boolean) VH.getAcquire(this)); }
    }

    @JCStressTest(Mode.Termination)
    @JCStressMeta(VarHandleBusyLoopTest.class)
    @State
    public static class Opaque_Volatile extends VarHandleBusyLoopTest {
        @Signal public void signal() { VH.setOpaque(this, true); }
        @Actor  public void actor1() { while (!(boolean) VH.getVolatile(this)); }
    }

    @JCStressTest(Mode.Termination)
    @JCStressMeta(VarHandleBusyLoopTest.class)
    @State
    public static class Release_Opaque extends VarHandleBusyLoopTest {
        @Signal public void signal() { VH.setRelease(this, true); }
        @Actor  public void actor1() { while (!(boolean) VH.getOpaque(this)); }
    }

    @JCStressTest(Mode.Termination)
    @JCStressMeta(VarHandleBusyLoopTest.class)
    @State
    public static class Release_Acquire extends VarHandleBusyLoopTest {
        @Signal public void signal() { VH.setRelease(this, true); }
        @Actor  public void actor1() { while (!(boolean) VH.getAcquire(this)); }
    }

    @JCStressTest(Mode.Termination)
    @JCStressMeta(VarHandleBusyLoopTest.class)
    @State
    public static class Release_Volatile extends VarHandleBusyLoopTest {
        @Signal public void signal() { VH.setRelease(this, true); }
        @Actor  public void actor1() { while (!(boolean) VH.getVolatile(this)); }
    }

    @JCStressTest(Mode.Termination)
    @JCStressMeta(VarHandleBusyLoopTest.class)
    @State
    public static class Volatile_Opaque extends VarHandleBusyLoopTest {
        @Signal public void signal() { VH.setVolatile(this, true); }
        @Actor  public void actor1() { while (!(boolean) VH.getOpaque(this)); }
    }

    @JCStressTest(Mode.Termination)
    @JCStressMeta(VarHandleBusyLoopTest.class)
    @State
    public static class Volatile_Acquire extends VarHandleBusyLoopTest {
        @Signal public void signal() { VH.setVolatile(this, true); }
        @Actor  public void actor1() { while (!(boolean) VH.getAcquire(this)); }
    }

    @JCStressTest(Mode.Termination)
    @JCStressMeta(VarHandleBusyLoopTest.class)
    @State
    public static class Volatile_Volatile extends VarHandleBusyLoopTest {
        @Signal public void signal() { VH.setVolatile(this, true); }
        @Actor  public void actor1() { while (!(boolean) VH.getVolatile(this)); }
    }

}
