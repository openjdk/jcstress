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
public class StateHolder<P> {
    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    public final boolean stopped;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    public final P[] pairs;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    public final int countWorkers;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    public final SpinLoopStyle spinStyle;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    private volatile int started;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    private volatile int ready;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    private volatile int finished;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    private volatile int consumed;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    private volatile boolean notAllStarted;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    private volatile boolean notAllReady;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    private volatile boolean notAllFinished;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    private volatile boolean notUpdated;

    @sun.misc.Contended
    @jdk.internal.vm.annotation.Contended
    public volatile boolean updateStride;

    static final AtomicIntegerFieldUpdater<StateHolder> UPDATER_STARTED  = AtomicIntegerFieldUpdater.newUpdater(StateHolder.class, "started");
    static final AtomicIntegerFieldUpdater<StateHolder> UPDATER_READY    = AtomicIntegerFieldUpdater.newUpdater(StateHolder.class, "ready");
    static final AtomicIntegerFieldUpdater<StateHolder> UPDATER_FINISHED = AtomicIntegerFieldUpdater.newUpdater(StateHolder.class, "finished");
    static final AtomicIntegerFieldUpdater<StateHolder> UPDATER_CONSUMED = AtomicIntegerFieldUpdater.newUpdater(StateHolder.class, "consumed");

    /**
     * Initial version
     */
    public StateHolder(P[] pairs, int expectedWorkers, SpinLoopStyle spinStyle) {
        this(false, pairs, expectedWorkers, spinStyle);
        updateStride = true;
    }

    /**
     * Updated version
     */
    public StateHolder(boolean stopped, P[] pairs, int expectedWorkers, SpinLoopStyle spinStyle) {
        this.stopped = stopped;
        this.pairs = pairs;
        this.countWorkers = expectedWorkers;
        this.spinStyle = spinStyle;
        UPDATER_STARTED.set(this, expectedWorkers);
        UPDATER_READY.set(this, expectedWorkers);
        UPDATER_FINISHED.set(this, expectedWorkers);
        UPDATER_CONSUMED.set(this, expectedWorkers);
        this.notAllReady = true;
        this.notAllFinished = true;
        this.notAllStarted = true;
        this.notUpdated = true;
    }

    public void preRun() {
        int v = UPDATER_READY.decrementAndGet(this);
        if (v == 0) {
            notAllReady = false;
        }

        switch (spinStyle) {
            case THREAD_YIELD:
                while (notAllReady) Thread.yield();
                break;
            case THREAD_SPIN_WAIT:
                while (notAllReady) Thread.onSpinWait();
                break;
            default:
                while (notAllReady);
        }

        if (UPDATER_STARTED.decrementAndGet(this) == 0) {
            notAllStarted = false;
        }
    }

    public void postRun() {
        if (UPDATER_FINISHED.decrementAndGet(this) == 0) {
            notAllFinished = false;
        }
        updateStride |= notAllStarted;

        switch (spinStyle) {
            case THREAD_YIELD:
                while (notAllFinished) Thread.yield();
                break;
            case THREAD_SPIN_WAIT:
                while (notAllFinished) Thread.onSpinWait();
                break;
            default:
                while (notAllFinished);
        }
    }

    public boolean tryStartUpdate()  {
        return (UPDATER_CONSUMED.decrementAndGet(this) == 0);
    }

    public void finishUpdate() {
        notUpdated = false;
    }

    public void postUpdate() {
        switch (spinStyle) {
            case THREAD_YIELD:
                while (notUpdated) Thread.yield();
                break;
            case THREAD_SPIN_WAIT:
                while (notUpdated) Thread.onSpinWait();
                break;
            default:
                while (notUpdated);
        }
    }

}
