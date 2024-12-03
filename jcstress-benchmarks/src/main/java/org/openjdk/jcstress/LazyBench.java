/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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

import org.openjdk.jcstress.samples.primitives.lazy.*;
import org.openjdk.jcstress.samples.primitives.lazy.shared.Lazy;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 5, jvmArgs = {"-Xmx1g", "-Xms1g"})
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class LazyBench {

    Lazy<Object> lazy;

    @Param({"broken-factory", "broken-nulls", "basic", "broken-one-shot", "wrapper-one-shot", "fenced-one-shot"})
    String impl;

    public Lazy<Object> createLazy() {
        switch (impl) {
            case "broken-factory":
                return new Lazy_01_BrokenFactory.BrokenFactoryLazy<>(() -> new Object());
            case "broken-nulls":
                return new Lazy_02_BrokenNulls.BrokenNullsLazy<>(() -> new Object());
            case "basic":
                return new Lazy_03_Basic.BasicLazy<>(() -> new Object());
            case "broken-one-shot":
                return new Lazy_04_BrokenOneShot.BrokenOneShotLazy<>(() -> new Object());
            case "wrapper-one-shot":
                return new Lazy_05_WrapperOneShot.FinalWrapperLazy<>(() -> new Object());
            case "fenced-one-shot":
                return new Lazy_06_FencedOneShot.FencedOneShot<>(() -> new Object());
            default:
                throw new IllegalArgumentException("Unknown factory: " + impl);
        }
    }

    @Setup
    public void setup() {
        lazy = createLazy();
    }

    @Benchmark
    public Object uncontended() {
        return lazy.get();
    }

    @Benchmark
    @Threads(Threads.MAX)
    public Object contended() {
        return lazy.get();
    }

}
