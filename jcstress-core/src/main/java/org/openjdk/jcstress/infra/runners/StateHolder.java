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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class StateHolder<P> {
    public final boolean stopped;
    public final P[] pairs;
    public final int countWorkers;
    public final SpinLoopStyle spinStyle;
    public final AtomicInteger started, ready, finished, consumed;
    public volatile boolean notAllStarted, notAllReady, notAllFinished, notUpdated;
    public volatile boolean hasLaggedWorkers;

    public StateHolder(boolean stopped, P[] pairs, int expectedWorkers, SpinLoopStyle spinStyle) {
        this.stopped = stopped;
        this.pairs = pairs;
        this.countWorkers = expectedWorkers;
        this.spinStyle = spinStyle;
        this.ready = new AtomicInteger(expectedWorkers);
        this.started = new AtomicInteger(expectedWorkers);
        this.finished = new AtomicInteger(expectedWorkers);
        this.consumed = new AtomicInteger(expectedWorkers);
        this.notAllReady = true;
        this.notAllFinished = true;
        this.notAllStarted = true;
        this.notUpdated = true;
    }

    public void preRun() {
        int v = ready.decrementAndGet();
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

        if (started.decrementAndGet() == 0) {
            notAllStarted = false;
        }
    }

    public void postRun() {
        if (finished.decrementAndGet() == 0) {
            notAllFinished = false;
        }
        hasLaggedWorkers |= notAllStarted;

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
        return (consumed.decrementAndGet() == 0);
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
