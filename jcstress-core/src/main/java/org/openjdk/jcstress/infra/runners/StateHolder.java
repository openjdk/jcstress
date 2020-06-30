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
package org.openjdk.jcstress.infra.runners;


import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class StateHolder<S, R> {

    // ------------------ Final, read-only fields ---------------------

    @sun.misc.Contended("finals")
    @jdk.internal.vm.annotation.Contended("finals")
    public final boolean stopped;

    @sun.misc.Contended("finals")
    @jdk.internal.vm.annotation.Contended("finals")
    public final S[] ss;

    @sun.misc.Contended("finals")
    @jdk.internal.vm.annotation.Contended("finals")
    public final R[] rs;

    @sun.misc.Contended("finals")
    @jdk.internal.vm.annotation.Contended("finals")
    public final SpinLoopStyle spinStyle;

    // --------------------- Write-frequent fields ------------------------
    // Threads are updating them frequently, and they need them completely
    // separate. Otherwise there are warmup/warmdown lags.

    @sun.misc.Contended("flags")
    @jdk.internal.vm.annotation.Contended("flags")
    public volatile boolean updateStride;

    @sun.misc.Contended("flags")
    @jdk.internal.vm.annotation.Contended("flags")
    private volatile int notStarted;

    @sun.misc.Contended("flags")
    @jdk.internal.vm.annotation.Contended("flags")
    private volatile int notFinished;

    @sun.misc.Contended("flags")
    @jdk.internal.vm.annotation.Contended("flags")
    private volatile int notConsumed;

    @sun.misc.Contended("flags")
    @jdk.internal.vm.annotation.Contended("flags")
    private volatile int notUpdated;

    static final AtomicIntegerFieldUpdater<StateHolder> UPDATER_NOT_STARTED = AtomicIntegerFieldUpdater.newUpdater(StateHolder.class, "notStarted");
    static final AtomicIntegerFieldUpdater<StateHolder> UPDATER_NOT_FINISHED = AtomicIntegerFieldUpdater.newUpdater(StateHolder.class, "notFinished");
    static final AtomicIntegerFieldUpdater<StateHolder> UPDATER_NOT_CONSUMED = AtomicIntegerFieldUpdater.newUpdater(StateHolder.class, "notConsumed");
    static final AtomicIntegerFieldUpdater<StateHolder> UPDATER_NOT_UPDATED = AtomicIntegerFieldUpdater.newUpdater(StateHolder.class, "notUpdated");

    /**
     * Initial version
     */
    public StateHolder(S[] states, R[] results, int expectedWorkers, SpinLoopStyle spinStyle) {
        this(false, states, results, expectedWorkers, spinStyle);
        updateStride = true;
    }

    /**
     * Updated version
     */
    public StateHolder(boolean stopped, S[] states, R[] results, int expectedWorkers, SpinLoopStyle spinStyle) {
        this.stopped = stopped;
        this.ss = states;
        this.rs = results;
        this.spinStyle = spinStyle;
        this.notStarted = expectedWorkers;
        this.notFinished = expectedWorkers;
        this.notConsumed = expectedWorkers;
        this.notUpdated = expectedWorkers;
    }

    public void preRun() {
        // Do not need to rendezvous the workers: first iteration would
        // probably lack any rendezvous, but all subsequent ones would
        // rendezvous during postUpdate().

        // Notify that we have started
        UPDATER_NOT_STARTED.decrementAndGet(this);
    }

    public void postRun() {
        // If any thread lags behind, then we need to update our stride
        if (!updateStride && notStarted > 0) {
            updateStride = true;
        }

        // Notify that we are finished
        UPDATER_NOT_FINISHED.decrementAndGet(this);

        switch (spinStyle) {
            case THREAD_YIELD:
                while (notFinished > 0) Thread.yield();
                break;
            case THREAD_SPIN_WAIT:
                while (notFinished > 0) Thread.onSpinWait();
                break;
            default:
                while (notFinished > 0);
        }
    }

    public boolean tryStartUpdate()  {
        return (UPDATER_NOT_CONSUMED.decrementAndGet(this) == 0);
    }

    public void postUpdate() {
        UPDATER_NOT_UPDATED.decrementAndGet(this);

        switch (spinStyle) {
            case THREAD_YIELD:
                while (notUpdated > 0) Thread.yield();
                break;
            case THREAD_SPIN_WAIT:
                while (notUpdated > 0) Thread.onSpinWait();
                break;
            default:
                while (notUpdated > 0);
        }
    }

}
