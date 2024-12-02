# Singleton Performance

While these samples are here to reason about correctness, a common question is also about the relative
performance of these implementations. Without going too deep into perf analysis, `SingletonBench` from
`jcstress-benchmarks` would show something like this on beefy AArch64 machine.

## Uncontended Case

```

Benchmark                                     (impl)  Mode  Cnt     Score      Error  Units

SingletonBench.uncontended            unsynchronized  avgt   25     0.886 ±    0.020  ns/op
SingletonBench.uncontended           broken-volatile  avgt   25     1.032 ±    0.032  ns/op
SingletonBench.uncontended           inefficient-cas  avgt   25     1.487 ±    0.027  ns/op
SingletonBench.uncontended  inefficient-synchronized  avgt   25    19.590 ±    0.243  ns/op
SingletonBench.uncontended                       dcl  avgt   25     0.913 ±    0.059  ns/op
SingletonBench.uncontended       acquire-release-dcl  avgt   25     0.989 ±    0.051  ns/op
SingletonBench.uncontended   broken-non-volatile-dcl  avgt   25     0.867 ±    0.014  ns/op
SingletonBench.uncontended             final-wrapper  avgt   25     1.038 ±    0.035  ns/op
SingletonBench.uncontended                    holder  avgt   25     0.654 ±    0.018  ns/op
SingletonBench.uncontended      thread-local-witness  avgt   25     2.710 ±    0.013  ns/op
```

### Holder

Holder is beyond reach, because JIT compilers are able to fold the whole thing to a
constant access.

```
   7.10%  ↗   0x0000ffff67ba0d00:   ldr        w10, [x15, #12]             ; get field `factory`
   7.10%  │   0x0000ffff67ba0d04:   ldr        w13, [x10, #8]              ; type-check `factory`
  25.75%  │   0x0000ffff67ba0d08:   cmp        w13, w14
          │   0x0000ffff67ba0d0c:   b.ne       0x0000ffff67ba0d74  
          |                       <poof: "constant load" was blackholed>
  22.83%  │   0x0000ffff67ba0d10:   ldarb      w10, [x12]                  ; JMH: get field `isDone`
   7.20%  │   0x0000ffff67ba0d14:   ldr        x13, [x28, #48]             ; JVM: safepoint poll, part 1
   0.00%  │   0x0000ffff67ba0d18:   add        x20, x20, #0x1              ; JMH: ops++
   7.13%  │   0x0000ffff67ba0d1c:   ldr        wzr, [x13]                  ; JVM: safepoint poll, part 2
  19.59%  ╰   0x0000ffff67ba0d20:   cbz        w10, 0x0000ffff67ba0d00     ; JMH: not done?
```

### DCLs

Double-checked locking idioms are nearly as fast. The cost they pay are going through
the acquire-load.

```
   4.04%  ↗   0x0000ffff9fb95880:   ldr        w10, [x15, #12]             ; get field `factory`
   4.81%  │   0x0000ffff9fb95884:   ldr        w14, [x10, #8]              ; type-check `factory`
  18.26%  │   0x0000ffff9fb95888:   cmp        w14, w12
          │   0x0000ffff9fb9588c:   b.ne       0x0000ffff9fb95904
  13.05%  │   0x0000ffff9fb95890:   mov        x13, x10                    ; get field `factory.instance`
   2.04%  │   0x0000ffff9fb95894:   add        x10, x13, #0xc
   4.20%  │   0x0000ffff9fb95898:   ldar       w10, [x10]                  ;    <--- note ldar (acquiring load)
   9.88%  │   0x0000ffff9fb9589c:   cbz        w10, 0x0000ffff9fb9592c     ; null-check `factory.instance`
          |                       <poof: w10 is our result, blackholed>
  10.19%  │   0x0000ffff9fb958a0:   ldarb      w14, [x11]                  ; JMH: get field `isDone`
   4.48%  │   0x0000ffff9fb958a4:   ldr        x10, [x28, #48]             ; JVM: safepoint poll, part 1
          │   0x0000ffff9fb958a8:   add        x19, x19, #0x1              ; JMH: ops++
   4.48%  │   0x0000ffff9fb958ac:   ldr        wzr, [x10]                  ; JVM: safepoint poll, part 2
  15.49%  ╰   0x0000ffff9fb958b0:   cbz        w14, 0x0000ffff9fb95880     ; JMH: not done?
```

### Wrappers (Final and CAS)

Wrappers are a little behind, because they need another memory dereference to reach the `instance`.

```
   3.92%  ↗   0x0000ffff6bdc5700:   ldr        w10, [x16, #12]             ; get field `factory`
   3.50%  │   0x0000ffff6bdc5704:   ldr        w14, [x10, #8]              ; type-check `factory`
   6.75%  │   0x0000ffff6bdc5708:   cmp        w14, w12
          │   0x0000ffff6bdc570c:   b.ne       0x0000ffff6bdc5780
   6.72%  │   0x0000ffff6bdc5710:   mov        x13, x10                    
   3.67%  │   0x0000ffff6bdc5714:   ldr        w10, [x13, #12]             ; get field `wrapper`
   4.60%  │   0x0000ffff6bdc5718:   ldr        w15, [x10, #12]             ; get field `wrapper.instance`
          │                      <poof: w15 is our result, blackholed>
  45.00%  │   0x0000ffff6bdc571c:   ldarb      w10, [x11]                  ; JMH: get field `isDone`
   3.41%  │   0x0000ffff6bdc5720:   mov        x13, x15                    
   3.40%  │   0x0000ffff6bdc5724:   ldr        x13, [x28, #48]             ; JVM: safepoint poll, part 1
   1.26%  │   0x0000ffff6bdc5728:   add        x19, x19, #0x1              ; JMH: ops++
   3.58%  │   0x0000ffff6bdc572c:   ldr        wzr, [x13]                  ; JVM: safepoint poll, part 2
   4.84%  ╰   0x0000ffff6bdc5730:   cbz        w10, 0x0000ffff6bdc5700     ; JMH: not done?
```

CAS example gets the same, but also pays for the cost of acquiring load.

### ThreadLocal

`ThreadLocal.get` on the fast path costs extra.

```
   1.67%  ↗   0x0000ffff6fdc6b60:   ldr        w11, [x13, #12]             ; get field `factory`
   1.67%  │   0x0000ffff6fdc6b64:   ldr        w12, [x11, #8]              ; type-check `factory`
   1.26%  │   0x0000ffff6fdc6b68:   cmp        w12, w10
          │   0x0000ffff6fdc6b6c:   b.ne       0x0000ffff6fdc6c2c  // b.any
   1.78%  │   0x0000ffff6fdc6b70:   mov        x15, x11                    
   1.22%  │   0x0000ffff6fdc6b74:   ldr        w12, [x15, #12]             ; ThreadLocal.get begins...
   1.46%  │   0x0000ffff6fdc6b78:   ldr        w18, [x16, #72]             
   1.56%  │   0x0000ffff6fdc6b7c:   ldr        w11, [x12, #8]              
   1.80%  │   0x0000ffff6fdc6b80:   cmp        w11, w1
          │   0x0000ffff6fdc6b84:   b.ne       0x0000ffff6fdc6c54  // b.any
   1.62%  │   0x0000ffff6fdc6b88:   mov        x0, x12                     
   1.69%  │   0x0000ffff6fdc6b8c:   ldr        w14, [x18, #20]             
   1.67%  │   0x0000ffff6fdc6b90:   ldr        w12, [x14, #12]             
   1.76%  │   0x0000ffff6fdc6b94:   ldr        w11, [x0, #12]
          │   0x0000ffff6fdc6b98:   sub        w4, w12, #0x1
   1.33%  │   0x0000ffff6fdc6b9c:   and        w11, w11, w4                
   1.47%  │   0x0000ffff6fdc6ba0:   mov        x4, x14                     
          │   0x0000ffff6fdc6ba4:   add        x4, x4, w11, sxtw #2
   1.33%  │   0x0000ffff6fdc6ba8:   cbz        w12, 0x0000ffff6fdc6c7c
   1.53%  │   0x0000ffff6fdc6bac:   ldr        w14, [x4, #16]              
   8.44%  │   0x0000ffff6fdc6bb0:   ldr        w12, [x14, #12]             
  34.02%  │   0x0000ffff6fdc6bb4:   cmp        x12, x0
          │   0x0000ffff6fdc6bb8:   b.ne       0x0000ffff6fdc6cb0          ; ThreadLocal.get ends.
  12.81%  │   0x0000ffff6fdc6bbc:   ldr        w11, [x14, #28]             ; get field `instance`
   2.22%  │   0x0000ffff6fdc6bc0:   cbz        w11, 0x0000ffff6fdc6cec    
          │                      <poof: w11 is our result, blackholed> 
   2.00%  │   0x0000ffff6fdc6bc4:   ldarb      w11, [x3]                   ; JMH: get field `isDone`
   1.46%  │   0x0000ffff6fdc6bc8:   ldr        w14, [x15, #16]
          │   0x0000ffff6fdc6bcc:   mov        x12, x14                    
   1.49%  │   0x0000ffff6fdc6bd0:   ldr        x12, [x28, #48]             ; JVM: safepoint poll, part 1 
          │   0x0000ffff6fdc6bd4:   add        x19, x19, #0x1              ; JMH: ops++
   1.47%  │   0x0000ffff6fdc6bd8:   ldr        wzr, [x12]                  ; JVM: safepoint poll, part 2
   1.42%  ╰   0x0000ffff6fdc6bdc:   cbz        w11, 0x0000ffff6fdc6b60     ; JMH: not done?
```

### Synchronized

Synchronized case takes a lock on fast-path and that lock acquisition costs extra.

The actual hot block would be CAS on acquring the object lock:

```
          ╭   0x0000ffff6fdc6158:   b.eq       0x0000ffff6fdc617c  // b.none
   0.18%  │   0x0000ffff6fdc615c:   ldr        x10, [x14]
   0.34%  │╭  0x0000ffff6fdc6160:   tbnz       w10, #1, 0x0000ffff6fdc618c
   0.54%  ││  0x0000ffff6fdc6164:   orr        x10, x10, #0x1
          ││  0x0000ffff6fdc6168:   eor        x11, x10, #0x1
   0.29%  ││  0x0000ffff6fdc616c:   mov        x8, x10
   0.36%  ││  0x0000ffff6fdc6170:   casa       x8, x11, [x14]
  24.92%  ││  0x0000ffff6fdc6174:   cmp        x8, x10
   0.18%  ││  0x0000ffff6fdc6178:   b.ne       0x0000ffff6fdc61b8  // b.any
   4.95%  ↘│  0x0000ffff6fdc617c:   str        x14, [x28, x12]
   0.22%   │  0x0000ffff6fdc6180:   add        w12, w12, #0x8
   0.20%   │  0x0000ffff6fdc6184:   str        w12, [x28, #1800]
   0.22%   │  0x0000ffff6fdc6188:   b          0x0000ffff6fdc61b8
           ↘  0x0000ffff6fdc618c:   add        x12, x10, #0x3e
```

## Contended Case

```
Benchmark                                     (impl)  Mode  Cnt     Score      Error  Units

SingletonBench.contended              unsynchronized  avgt   25     0.888 ±    0.013  ns/op
SingletonBench.contended             broken-volatile  avgt   25     1.047 ±    0.021  ns/op
SingletonBench.contended             inefficient-cas  avgt   25     1.495 ±    0.011  ns/op
SingletonBench.contended    inefficient-synchronized  avgt   25  6854.528 ± 1603.611  ns/op
SingletonBench.contended                         dcl  avgt   25     0.941 ±    0.044  ns/op
SingletonBench.contended         acquire-release-dcl  avgt   25     1.030 ±    0.085  ns/op
SingletonBench.contended     broken-non-volatile-dcl  avgt   25     0.901 ±    0.019  ns/op
SingletonBench.contended               final-wrapper  avgt   25     1.056 ±    0.020  ns/op
SingletonBench.contended                      holder  avgt   25     0.689 ±    0.028  ns/op
SingletonBench.contended        thread-local-witness  avgt   25     2.679 ±    0.031  ns/op
```

All tests behave similarly under contention, except `synchronized` case, which falls victim
to major scalability bottleneck.
