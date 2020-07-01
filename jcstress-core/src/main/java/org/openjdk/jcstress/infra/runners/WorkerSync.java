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
import java.util.concurrent.locks.LockSupport;

/**
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
@sun.misc.Contended
@jdk.internal.vm.annotation.Contended
public class WorkerSync {

    public final boolean stopped;
    public final SpinLoopStyle spinStyle;

    public volatile boolean updateStride;
    private volatile int notStarted;
    private volatile int notFinished;
    private volatile int notConsumed;
    private volatile int notUpdated;

    static final AtomicIntegerFieldUpdater<WorkerSync> UPDATER_NOT_STARTED = AtomicIntegerFieldUpdater.newUpdater(WorkerSync.class, "notStarted");
    static final AtomicIntegerFieldUpdater<WorkerSync> UPDATER_NOT_FINISHED = AtomicIntegerFieldUpdater.newUpdater(WorkerSync.class, "notFinished");
    static final AtomicIntegerFieldUpdater<WorkerSync> UPDATER_NOT_CONSUMED = AtomicIntegerFieldUpdater.newUpdater(WorkerSync.class, "notConsumed");
    static final AtomicIntegerFieldUpdater<WorkerSync> UPDATER_NOT_UPDATED = AtomicIntegerFieldUpdater.newUpdater(WorkerSync.class, "notUpdated");

    public WorkerSync(boolean stopped, int expectedWorkers, SpinLoopStyle spinStyle) {
        this.stopped = stopped;
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
            case HARD:
                while (notFinished > 0);
                break;
            case THREAD_YIELD:
                while (notFinished > 0) Thread.yield();
                break;
            case THREAD_SPIN_WAIT:
                while (notFinished > 0) Thread.onSpinWait();
                break;
            case LOCKSUPPORT_PARK_NANOS:
                while (notFinished > 0) LockSupport.parkNanos(1);
                break;
            default:
                throw new IllegalStateException("Unhandled style: " + spinStyle);
        }
    }

    public boolean tryStartUpdate()  {
        return (UPDATER_NOT_CONSUMED.decrementAndGet(this) == 0);
    }

    public void postUpdate() {
        UPDATER_NOT_UPDATED.decrementAndGet(this);

        switch (spinStyle) {
            case HARD:
                while (notUpdated > 0);
                break;
            case THREAD_YIELD:
                while (notUpdated > 0) Thread.yield();
                break;
            case THREAD_SPIN_WAIT:
                while (notUpdated > 0) Thread.onSpinWait();
                break;
            case LOCKSUPPORT_PARK_NANOS:
                while (notUpdated > 0) LockSupport.parkNanos(1);
                break;
            default:
                throw new IllegalStateException("Unhandled style: " + spinStyle);
        }
    }

}
