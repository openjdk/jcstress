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

import org.openjdk.jcstress.samples.primitives.singletons.shared.FinalSingleton;
import org.openjdk.jcstress.samples.primitives.singletons.*;
import org.openjdk.jcstress.samples.primitives.singletons.shared.Factory;
import org.openjdk.jcstress.samples.primitives.singletons.shared.Singleton;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 5, jvmArgs = {"-Xmx1g", "-Xms1g"})
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class SingletonBench {

    Factory<Singleton> factory;

    @Param({"unsynchronized", "broken-volatile", "inefficient-cas", "inefficient-synchronized",
            "dcl", "acquire-release-dcl", "broken-non-volatile-dcl", "final-wrapper", "holder",
            "thread-local-witness"})
    String impl;

    public Factory<Singleton> createFactory() {
        switch (impl) {
            case "unsynchronized":
                return new Singleton_01_BrokenUnsynchronized.Unsynchronized<>();
            case "broken-volatile":
                return new Singleton_02_BrokenVolatile.VolatileS<>();
            case "inefficient-cas":
                return new Singleton_03_InefficientCAS.CAS<>();
            case "inefficient-synchronized":
                return new Singleton_04_InefficientSynchronized.Synchronized<>();
            case "dcl":
                return new Singleton_05_DCL.DCL<>();
            case "acquire-release-dcl":
                return new Singleton_06_AcquireReleaseDCL.AcquireReleaseDCL<>();
            case "broken-non-volatile-dcl":
                return new Singleton_07_BrokenNonVolatileDCL.NonVolatileDCL<>();
            case "final-wrapper":
                return new Singleton_08_FinalWrapper.FinalWrapper<>();
            case "holder":
                return new Singleton_09_Holder.FinalHolderHolder();
            case "thread-local-witness":
                return new Singleton_10_ThreadLocalWitness.ThreadLocalWitness<>();
            default:
                throw new IllegalArgumentException("Unknown factory: " + impl);
        }
    }

    @Setup
    public void setup() {
        factory = createFactory();
    }

    @Benchmark
    public Object uncontended() {
        return factory.get(() -> new FinalSingleton("data"));
    }

    @Benchmark
    @Threads(Threads.MAX)
    public Object contended() {
        return factory.get(() -> new FinalSingleton("data"));
    }

}
