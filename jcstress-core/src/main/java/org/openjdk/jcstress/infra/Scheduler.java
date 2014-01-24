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
package org.openjdk.jcstress.infra;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class Scheduler {

    private final Semaphore sentinel;

    private final ExecutorService services = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setPriority(Thread.MAX_PRIORITY);
            t.setDaemon(true);
            return t;
        }
    });

    public Scheduler(int totalTokens) {
        this.sentinel = new Semaphore(totalTokens);
    }

    public void schedule(final ScheduledTask task) throws InterruptedException {
        sentinel.acquire(task.getTokens());
        services.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    task.run();
                } finally {
                    sentinel.release(task.getTokens());
                }
            }
        });
    }

    public void waitFinish() throws InterruptedException {
        services.shutdown();
        services.awaitTermination(1, TimeUnit.DAYS);
    }

    public interface ScheduledTask extends Runnable {
        int getTokens();
    }

}
