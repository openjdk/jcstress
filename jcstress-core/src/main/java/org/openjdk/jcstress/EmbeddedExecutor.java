/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress;

import org.openjdk.jcstress.infra.Status;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.collectors.TestResultCollector;
import org.openjdk.jcstress.infra.runners.Runner;
import org.openjdk.jcstress.infra.runners.TestConfig;
import org.openjdk.jcstress.util.StringUtils;

import java.lang.reflect.Constructor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class EmbeddedExecutor {

    private final ExecutorService pool;
    private final TestResultCollector sink;
    private Consumer<TestConfig> onFinish;

    public EmbeddedExecutor(TestResultCollector sink) {
        this(sink, null);
    }

    public EmbeddedExecutor(TestResultCollector sink, Consumer<TestConfig> onFinish) {
        this.sink = sink;
        this.onFinish = onFinish;
        pool = Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger id = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("jcstress-worker-" + id.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        });
    }

    public void submit(TestConfig config) {
        pool.submit(task(config));
    }

    public void run(TestConfig config) {
        task(config).run();
    }

    private Runnable task(TestConfig config) {
        return () -> {
            try {
                Class<?> aClass = Class.forName(config.generatedRunnerName);
                Constructor<?> cnstr = aClass.getConstructor(TestConfig.class, TestResultCollector.class, ExecutorService.class);
                Runner<?> o = (Runner<?>) cnstr.newInstance(config, sink, pool);
                o.run();
            } catch (ClassFormatError | NoClassDefFoundError | NoSuchMethodError | NoSuchFieldError e) {
                TestResult result = new TestResult(config, Status.API_MISMATCH, 0);
                result.addAuxData(StringUtils.getStacktrace(e));
                sink.add(result);
            } catch (Throwable ex) {
                TestResult result = new TestResult(config, Status.TEST_ERROR, 0);
                result.addAuxData(StringUtils.getStacktrace(ex));
                sink.add(result);
            } finally {
                if (onFinish != null) {
                    onFinish.accept(config);
                }
            }
        };
    }
}
