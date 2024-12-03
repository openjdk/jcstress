# Lazy Performance

While these samples are here to reason about correctness, a common question is also about the relative
performance of these implementations. Without going too deep into perf analysis, `LazyBench` from
`jcstress-benchmarks` would show something like this on beefy AArch64 machine.

## Uncontended Case

```
Benchmark                        (impl)  Mode  Cnt  Score    Error  Units
LazyBench.uncontended    broken-factory  avgt   25  1.004 ±  0.080  ns/op
LazyBench.uncontended      broken-nulls  avgt   25  0.891 ±  0.022  ns/op
LazyBench.uncontended             basic  avgt   25  1.038 ±  0.030  ns/op
LazyBench.uncontended   broken-one-shot  avgt   25  1.040 ±  0.045  ns/op
LazyBench.uncontended  wrapper-one-shot  avgt   25  1.295 ±  0.048  ns/op
LazyBench.uncontended   fenced-one-shot  avgt   25  1.068 ±  0.027  ns/op
```

### Broken Nulls

This test is ahead of other tests, because it has to do only a single read on the fast path:

```
   5.49%  ↗   0x0000ffff6fed14c0:   ldr        w10, [x15, #12]             ; get field "lazy"
   4.99%  │   0x0000ffff6fed14c4:   ldr        w14, [x10, #8]              ; typecheck for Lazy subtype
  15.96%  │   0x0000ffff6fed14c8:   cmp        w14, w12
          │   0x0000ffff6fed14cc:   b.ne       0x0000ffff6fed1540  // b.any
  15.35%  │   0x0000ffff6fed14d0:   mov        x13, x10
   1.92%  │   0x0000ffff6fed14d4:   add        x10, x13, #0x10
   5.02%  │   0x0000ffff6fed14d8:   ldar       w10, [x10]                  ; get field "instance"     <--- HERE
   5.93%  │   0x0000ffff6fed14dc:   cbz        w10, 0x0000ffff6fed1568     ; null check "instance"
          |                       <poof: our result is in w10, blackholed>
  16.60%  │   0x0000ffff6fed14e0:   ldarb      w14, [x11]                  ; JMH: get field "isDone"
   5.15%  │   0x0000ffff6fed14e4:   ldr        x10, [x28, #48]             ; JVM: safepoint poll, part 1
          │   0x0000ffff6fed14e8:   add        x19, x19, #0x1              ; JMH: ops++
   4.78%  │   0x0000ffff6fed14ec:   ldr        wzr, [x10]                  ; JVM: safepoint poll, part 2
  10.98%  ╰   0x0000ffff6fed14f0:   cbz        w14, 0x0000ffff6fed14c0     ; JMH: loop
```

### Basic

The variants of `basic` test, including `broken-one-shot`, `fenced-one-shot`, `broken-factory` show the same fast path,
where the cost is the atomic load of `factory`, and non-atomic load of `instance`:

```
   4.76%  ↗   0x0000ffff83ed09c0:   ldr        w11, [x15, #12]             ; get field "lazy"
   4.30%  │   0x0000ffff83ed09c4:   ldr        w13, [x11, #8]              ; typecheck for Lazy subtype
  16.76%  │   0x0000ffff83ed09c8:   cmp        w13, w10
          │   0x0000ffff83ed09cc:   b.ne       0x0000ffff83ed0a44
  13.52%  │   0x0000ffff83ed09d0:   add        x13, x11, #0xc
   4.67%  │   0x0000ffff83ed09d4:   ldarb      w14, [x13]                  ; get field "factory"       <--- HERE
   4.83%  │   0x0000ffff83ed09d8:   cbz        w14, 0x0000ffff83ed0a6c     ; null check "factory"
  13.45%  │   0x0000ffff83ed09dc:   ldarb      w13, [x12]                  ; JMH: get field "isDone"
   4.06%  │   0x0000ffff83ed09e0:   ldr        w14, [x11, #20]             ; get field "instance"      <--- AND HERE
   0.50%  │   0x0000ffff83ed09e4:   mov        x11, x14                    
          |                       <poof: our result is in x11, blackholed>
   4.43%  │   0x0000ffff83ed09e8:   ldr        x14, [x28, #48]             ; JVM: safepoint poll, part 1
   0.55%  │   0x0000ffff83ed09ec:   add        x19, x19, #0x1              ; JMH: ops++
   4.10%  │   0x0000ffff83ed09f0:   ldr        wzr, [x14]                  ; JVM: safepoint poll, part 2
  15.08%  ╰   0x0000ffff83ed09f4:   cbz        w13, 0x0000ffff83ed09c0     ; JMH: loop
```

### Wrapper One Shot

This test is a bit behind other tests, because it has to do the additional memory dereference on fast path:

```
   3.70%  ↗   0x0000ffff83ed0510:   ldr        w10, [x13, #12]             ; get field "lazy"
   3.19%  │   0x0000ffff83ed0514:   ldr        w12, [x10, #8]              ; typecheck for Lazy subtype
   4.23%  │   0x0000ffff83ed0518:   cmp        w12, w15
          │   0x0000ffff83ed051c:   b.ne       0x0000ffff83ed05c8  // b.any
   4.21%  │   0x0000ffff83ed0520:   mov        x12, x10
   3.22%  │   0x0000ffff83ed0524:   ldr        w10, [x12, #12]             ; get field "wrapper"      <--- HERE
   0.46%  │   0x0000ffff83ed0528:   mov        x14, x10                    ; null check "wrapper"
   2.84%  │   0x0000ffff83ed052c:   cbz        w10, 0x0000ffff83ed05a4
   3.50%  │   0x0000ffff83ed0530:   add        x10, x14, #0xc
   3.74%  │   0x0000ffff83ed0534:   ldar       w10, [x10]                  ; get field "factory"      <--- AND HERE
  30.34%  │   0x0000ffff83ed0538:   cbnz       w10, 0x0000ffff83ed05f0     ; null check "factory"
  15.99%  │   0x0000ffff83ed053c:   ldarb      w14, [x11]                  : JMH: get field "isDone"
   3.35%  │   0x0000ffff83ed0540:   ldr        w12, [x12, #16]             ; get field "instance"     <--- AND HERE
   0.57%  │   0x0000ffff83ed0544:   mov        x10, x12
          |                       <poof: our result is in x10, blackholed>
   3.65%  │   0x0000ffff83ed0548:   ldr        x12, [x28, #48]             ; JVM: safepoint poll, part 1
          │   0x0000ffff83ed054c:   add        x19, x19, #0x1              ; JMH: ops++
   3.85%  │   0x0000ffff83ed0550:   ldr        wzr, [x12]                  ; JVM: safepoint poll, part 2
   3.90%  ╰   0x0000ffff83ed0554:   cbz        w14, 0x0000ffff83ed0510     ; JMH: loop
```

## Contended Case

```
Benchmark                        (impl)  Mode  Cnt  Score    Error  Units
LazyBench.contended      broken-factory  avgt   25  1.011 ±  0.048  ns/op
LazyBench.contended        broken-nulls  avgt   25  0.903 ±  0.016  ns/op
LazyBench.contended               basic  avgt   25  1.027 ±  0.023  ns/op
LazyBench.contended     broken-one-shot  avgt   25  1.016 ±  0.021  ns/op
LazyBench.contended    wrapper-one-shot  avgt   25  1.295 ±  0.013  ns/op
LazyBench.contended     fenced-one-shot  avgt   25  1.038 ±  0.026  ns/op
```

As expected, all test behave similarly under contention.
