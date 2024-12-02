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
