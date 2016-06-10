/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress.chapters;


import static java.util.Map.entry;
import static java.util.Set.of;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Source.DATA_SOURCE;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Source.VIEW_SOURCE;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Target.*;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Target.Operation.*;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Type.SHORT;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Type.CHAR;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Type.INT;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Type.LONG;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Type.FLOAT;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Type.DOUBLE;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Type.STRING;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.Get;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.GetVolatile;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.GetOpaque;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.GetAcquire;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.Set;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.SetVolatile;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.SetOpaque;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.SetRelease;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.FullFence;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.AcquireFence;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.ReleaseFence;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.LoadLoadFence;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.StoreStoreFence;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.GetAndAdd;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.AddAndGet;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.CompareAndSet;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.CompareAndExchangeVolatile;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.CompareAndExchangeAcquire;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.CompareAndExchangeRelease;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.WeakCompareAndSet;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.WeakCompareAndSetAcquire;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.WeakCompareAndSetRelease;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.WeakCompareAndSetVolatile;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.GetAndSet;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.Type.GET_SET;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.Type.STATIC_FENCE;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.Type.ATOMIC_UPDATE;
import static org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Method.Type.NUMERIC_ATOMIC_UPDATE;

import org.openjdk.jcstress.chapters.Chapter1bTestGenerator.Target.Operation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openjdk.jcstress.Spp;
import org.openjdk.jcstress.Values;

import static org.openjdk.jcstress.chapters.GeneratorUtils.readFromResource;
import static org.openjdk.jcstress.chapters.GeneratorUtils.upcaseFirst;
import static org.openjdk.jcstress.chapters.GeneratorUtils.writeOut;

public class Chapter1bTestGenerator {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalStateException("Need a destination argument");
        }
        String dest = args[0];

        for(Entry<String, Target> template : TEMPLATES.entrySet()) {
            final String templateName = template.getKey();
            final Target target = template.getValue();

            makeFieldTests(dest, target, "fields", templateName,
                    readFromResource("/chapter1b/fields/" + templateName + ".java.template"));

            makeArrayTests(dest, target, "arrays", templateName,
                    readFromResource("/chapter1b/arrays/" + templateName + ".java.template"));

            makeByteBufferTests(dest, target, "byteArray", "byteArray", BufferType.ARRAY, templateName,
                    readFromResource("/chapter1b/byteArray/" + templateName + ".java.template"));

            makeByteBufferTests(dest, target, "byteBuffer", "byteBuffer", BufferType.HEAP, templateName,
                    readFromResource("/chapter1b/byteBuffer/" + templateName + ".java.template"));

            makeByteBufferTests(dest, target, "byteBuffer", "byteBuffer", BufferType.DIRECT, templateName,
                    readFromResource("/chapter1b/byteBuffer/" + templateName + ".java.template"));
        }
    }

    private static void makeFieldTests(String dest, Target target, String vhType, String templateName, String template) throws IOException {
        for (Type type : DATA_SOURCE.supportedTypes()) {
            for (Operation operation : target.operations) {
                if (!DATA_SOURCE.supported(operation.method, type))
                    break;

                String result = generateConcreteOperation(template, target.target, operation.operation,
                        "field", "field = $1;");

                String pkg = BASE_PKG + "." + vhType + "." + templateName;
                String testName = operation.method.name() + upcaseFirst(type.type);

                writeOut(dest, pkg, testName, Spp.spp(result,
                        keys(type),
                        fieldVars(type, "this", pkg, testName)
                ));
            }
        }
    }

    private static void makeArrayTests(String dest, Target target, String vhType, String templateName, String template) throws IOException {
        for (Type type : DATA_SOURCE.supportedTypes()) {
            for (Operation operation : target.operations) {
                if (!DATA_SOURCE.supported(operation.method, type))
                    break;

                String result = generateConcreteOperation(template, target.target, operation.operation,
                        "array[0]", "array[0] = $1;");

                String pkg = BASE_PKG + "." + vhType + "." + templateName;
                String testName = operation.method.name() + upcaseFirst(type.type);

                writeOut(dest, pkg, testName, Spp.spp(result,
                        keys(type),
                        arrayVars(type, "array", pkg, testName)
                ));
            }
        }
    }

    private static void makeByteBufferTests(String dest, Target target, String vhType, String object,
            BufferType bufferType, String templateName, String template)
            throws IOException {
        for (Type type : VIEW_SOURCE.supportedTypes()) {
            for (Operation operation : target.operations) {
                if (!VIEW_SOURCE.supported(operation.method, type))
                    break;

                String result = generateConcreteOperation(template, target.target, operation.operation,
                        "(\\$type\\$) vh.get(\\$object\\$\\$index_para\\$)",
                        "vh.set(\\$object\\$\\$index_para\\$, $1);");

                String pkg = BASE_PKG + "." + vhType + bufferType.pkgAppendix() + "." + templateName;
                String testName = operation.method.name() + upcaseFirst(type.type);

                writeOut(dest, pkg, testName, Spp.spp(result,
                        keys(type),
                        viewVars(type, object, pkg, testName, bufferType.allocateOp)
                ));
            }
        }
    }

    private static String generateConcreteOperation(String result, String target, String concreteOp,
            String getOp, String setOp) {
        result = result.replaceAll(target, concreteOp);
        return result.replaceAll("%GetVar%", getOp)
                .replaceAll("%SetVar<(.+)>%", setOp)
                .replaceAll("%CompareAndSet<(.+), (.+)>%", COMPAREANDSET.operation);
    }

    private static Set<String> keys(Type type) {
        if (type.alwaysAtomic)
            return of("alwaysAtomic");
        else
            return of();
    }

    private static Map<String, String> commonVars(Type type, String object, String pkg, String testName) {
        Map<String, String> map = new HashMap<>();
        map.put("type", type.type);
        map.put("Type", upcaseFirst(type.type));
        map.put("TestClassName", testName);
        map.put("package", pkg);
        map.put("object", object);

        for (int i = 0; i < type.values.length; i++) {
            map.put("value" + i, type.values[i]);
            map.put("valueLiteral" + i, type.valueLiterals[i]);
        }

        return map;
    }

    private static Map<String, String> fieldVars(Type type, String object, String pkg, String testName) {
        Map<String, String> map = commonVars(type, object, pkg, testName);
        map.put("index_para", "");
        return map;
    }

    private static Map<String, String> arrayVars(Type type, String object, String pkg, String testName) {
        Map<String, String> map = commonVars(type, object, pkg, testName);
        map.put("index_para", ", 0");
        return map;
    }

    private static Map<String, String> viewVars(Type type, String object, String pkg, String testName,
            String bufferAllocateOp) {
        Map<String, String> map = commonVars(type, object, pkg, testName);
        map.put("index_para", ", 0");
        map.put("unit_size", String.valueOf(type.sizeInArray));
        map.put("buffer_allocate", bufferAllocateOp);
        return map;
    }

    enum Type {
        INT("int",
                Integer.BYTES,
                true,

                // here value2 should equal value0 + value1 + value1, to meet GetAndAdd, AddAndGet tests
                new String[] { "-2", "7" },
                new String[] { "-2", "7" }
        ),

        LONG("long",
                Long.BYTES,
                false,

                // here value2 should equal value0 + value1 + value1, to meet GetAndAdd, AddAndGet tests
                new String[] { "-2L", "7" },
                new String[] { "-2", "7" }
        ),

        STRING("String",
                4,
                true,
                // reference may be compressed on 64-bits, 4 is to make sure the real total bytes exceed 256
                new String[] { "\"2\"", "\"7\"" },
                new String[] { "2", "7" }
        ),

        BOOLEAN("boolean",
                1,
                true,
                new String[] {},
                new String[] {}
        ),

        BYTE("byte",
                Byte.BYTES,
                true,
                new String[] {},
                new String[] {}
        ),

        SHORT("short",
                Short.BYTES,
                true,
                new String[] {},
                new String[] {}
        ),

        CHAR("char",
                Character.BYTES,
                true,
                new String[] {},
                new String[] {}
        ),

        FLOAT("float",
                Float.BYTES,
                true,
                new String[] { "2", "7" },
                new String[] { "2.0", "7.0" }
        ),

        DOUBLE("double",
                Double.BYTES,
                false,
                new String[] { "2", "7" },
                new String[] { "2.0", "7.0" }
        );

        Type(String type, int sizeInArray, boolean alwaysAtomic, String[] extraValueLiterals,
                String[] extraValues) {
            this.type = type;
            this.sizeInArray = sizeInArray;
            this.alwaysAtomic = alwaysAtomic;

            valueLiterals = new String[extraValueLiterals.length + 2];
            valueLiterals[0] = Values.DEFAULTS_LITERAL.get(type);
            valueLiterals[1] = Values.VALUES_LITERAL.get(type);
            System.arraycopy(extraValueLiterals, 0, valueLiterals, 2, extraValueLiterals.length);

            values = new String[extraValues.length + 2];
            values[0] = Values.DEFAULTS.get(type);
            values[1] = Values.VALUES.get(type);
            System.arraycopy(extraValues, 0, values, 2, extraValues.length);
        }

        String type;
        int sizeInArray;
        boolean alwaysAtomic;
        String[] valueLiterals;
        String[] values;
    }

    static abstract class Source {
        protected abstract Type[] supportedTypes();

        protected abstract boolean supported(Method method, Type type);

        boolean commonSupported(Method method) {
            return (method.type == GET_SET || method.type == STATIC_FENCE);
        }

        static final Source DATA_SOURCE = new Source() {

            @Override
            protected Type[] supportedTypes() {
                return Type.values();
            }

            @Override
            protected boolean supported(Method method, Type type) {
                if (commonSupported(method))
                    return true;

                if ((type == INT || type == LONG || type == STRING) && method.type == ATOMIC_UPDATE)
                    return true;

                if ((type == INT || type == LONG) && method.type == NUMERIC_ATOMIC_UPDATE)
                    return true;

                return false;
            }

        };

        static final Source VIEW_SOURCE = new Source() {

            @Override
            protected Type[] supportedTypes() {
                return VIEW_SUPPORTED_TYPES;
            }

            @Override
            protected boolean supported(Method method, Type type) {
                if (commonSupported(method))
                    return true;

                if ((type == INT || type == LONG || type == FLOAT || type == DOUBLE)
                        && method.type == ATOMIC_UPDATE)
                    return true;

                if ((type == INT || type == LONG) && method.type == NUMERIC_ATOMIC_UPDATE)
                    return true;

                return false;
            }

            private final Type[] VIEW_SUPPORTED_TYPES = new Type[] { SHORT, CHAR, INT, LONG, FLOAT, DOUBLE };

        };

    }

    enum Method {
        Get(GET_SET),
        GetVolatile(GET_SET),
        GetOpaque(GET_SET),
        GetAcquire(GET_SET),

        Set(GET_SET),
        SetVolatile(GET_SET),
        SetOpaque(GET_SET),
        SetRelease(GET_SET),

        FullFence(STATIC_FENCE),
        AcquireFence(STATIC_FENCE),
        ReleaseFence(STATIC_FENCE),
        LoadLoadFence(STATIC_FENCE),
        StoreStoreFence(STATIC_FENCE),

        CompareAndSet(ATOMIC_UPDATE),
        CompareAndExchangeVolatile(ATOMIC_UPDATE),
        CompareAndExchangeAcquire(ATOMIC_UPDATE),
        CompareAndExchangeRelease(ATOMIC_UPDATE),

        WeakCompareAndSet(ATOMIC_UPDATE),
        WeakCompareAndSetAcquire(ATOMIC_UPDATE),
        WeakCompareAndSetRelease(ATOMIC_UPDATE),
        WeakCompareAndSetVolatile(ATOMIC_UPDATE),
        GetAndSet(ATOMIC_UPDATE),

        GetAndAdd(NUMERIC_ATOMIC_UPDATE),
        AddAndGet(NUMERIC_ATOMIC_UPDATE);

        Method(Type type) {
            this.type = type;
        }

        Type type;

        enum Type {
            GET_SET, STATIC_FENCE, ATOMIC_UPDATE, NUMERIC_ATOMIC_UPDATE;
        }
    }

    private static final String LNSEP = System.getProperty("line.separator");

    enum Target {
        T_ADDANDGET(
                "%AddAndGet<(.+)>%",
                of(ADDANDGET)
        ),

        T_GETANDADD(
                "%GetAndAdd<(.+)>%",
                of(GETANDADD)
        ),

        T_GETANDSET(
                "%GetAndSet<(.+)>%",
                of(GETANDSET)
        ),

        T_CAE(
                "%CAE<(.+), (.+)>%",
                of(COMPAREANDEXCHANGEVOLATILE, COMPAREANDEXCHANGERELEASE, COMPAREANDEXCHANGEACQUIRE)
        ),

        T_CAS(
                "%CAS<(.+), (.+)>%",
                of(COMPAREANDSET)
        ),

        T_WEAKCAS(
                "%WeakCAS<(.+), (.+)>%",
                of(WEAKCOMPAREANDSETRELEASE, WEAKCOMPAREANDSETACQUIRE, WEAKCOMPAREANDSET, WEAKCOMPAREANDSETVOLATILE)
        ),

        T_GET(
                "%Get<>%",
                of(GET, COMPAREANDEXCHANGERELEASE_FAIL, WEAKCOMPAREANDSET_RETURN, WEAKCOMPAREANDSETRELEASE_RETURN)
        ),

        T_SET(
                "%Set<(.+)>%",
                of(SET, WEAKCOMPAREANDSET_SUC, COMPAREANDEXCHANGEACQUIRE_SUC, WEAKCOMPAREANDSETACQUIRE_SUC)),

        T_SETOPAQUE(
                "%SetOpaque<(.+)>%",
                of(SETOPAQUE)
        ),

        T_GETOPAQUE(
                "%GetOpaque<>%",
                of(GETOPAQUE)
        ),

        T_GET_LOADLOADFENCE(
                "%GetLoadLoadFence<>%",
                of(GET_ACQUIREFENCE, GET_LOADLOADFENCE, GET_FULLFENCE)
        ),

        T_LOADSTOREFENCE_SET(
                "%LoadStoreFenceSet<(.+)>%",
                of(RELEASEFENCE_SET, FULLFENCE_SET)
        ),

        T_GET_LOADSTOREFENCE(
                "%GetLoadStoreFence<>%",
                of(GET_ACQUIREFENCE, GET_FULLFENCE)
        ),

        T_STORESTOREFENCE_SET(
                "%StoreStoreFenceSet<(.+)>%",
                of(RELEASEFENCE_SET, STORESTOREFENCE_SET, FULLFENCE_SET)
        ),

        T_SET_STORESTOREFENCE(
                "%SetStoreStoreFence<(.+)>%",
                of(SET_RELEASEFENCE, SET_STORESTOREFENCE, SET_FULLFENCE)
        ),

        T_SET_STORELOADFENCE(
                "%SetStoreLoadFence<(.+)>%",
                of(SET_FULLFENCE)
        ),

        ;

        Target(String target, Set<Operation> operations) {
            this.target = target;
            this.operations = operations;
        }

        String target;
        Set<Operation> operations;

        enum Operation {
            SETVOLATILE(
                    "vh.setVolatile(\\$object\\$\\$index_para\\$, $1);",
                    SetVolatile
            ),

            SETRELEASE(
                    "vh.setRelease(\\$object\\$\\$index_para\\$, $1);",
                    SetRelease
            ),

            COMPAREANDSET_SUC(
                    "vh.compareAndSet(\\$object\\$\\$index_para\\$, \\$valueLiteral0\\$, $1);",
                    CompareAndSet
            ),

            COMPAREANDEXCHANGEVOLATILE_SUC(
                    "vh.compareAndExchangeVolatile(\\$object\\$\\$index_para\\$, \\$valueLiteral0\\$, $1);",
                    CompareAndExchangeVolatile
            ),

            COMPAREANDEXCHANGERELEASE_SUC(
                    "vh.compareAndExchangeRelease(\\$object\\$\\$index_para\\$, \\$valueLiteral0\\$, $1);",
                    CompareAndExchangeRelease
            ),

            WEAKCOMPAREANDSETRELEASE_SUC(
                    "vh.weakCompareAndSetRelease(\\$object\\$\\$index_para\\$, \\$valueLiteral0\\$, $1);",
                    WeakCompareAndSetRelease
            ),

            WEAKCOMPAREANDSETVOLATILE_SUC(
                    "vh.weakCompareAndSetVolatile(\\$object\\$\\$index_para\\$, \\$valueLiteral0\\$, $1);",
                    WeakCompareAndSetVolatile
            ),

            ADDANDGET(
                    "vh.addAndGet(\\$object\\$\\$index_para\\$, $1);",
                    AddAndGet
            ),

            GETANDADD(
                    "vh.getAndAdd(\\$object\\$\\$index_para\\$, $1);",
                    GetAndAdd
            ),

            GETANDSET(
                    "vh.getAndSet(\\$object\\$\\$index_para\\$, $1);",
                    GetAndSet
            ),

            GETVOLATILE(
                    "(\\$type\\$) vh.getVolatile(\\$object\\$\\$index_para\\$);",
                    GetVolatile
            ),

            GETACQUIRE(
                    "(\\$type\\$) vh.getAcquire(\\$object\\$\\$index_para\\$);",
                    GetAcquire
            ),

            GET_COMPAREANDSET_FAIL(
                    "%GetVar%;" + LNSEP + "vh.compareAndSet(\\$object\\$\\$index_para\\$, \\$valueLiteral3\\$, \\$valueLiteral3\\$);",
                    CompareAndSet
            ),

            ADDANDGET_ZERO(
                    "(\\$type\\$) vh.addAndGet(\\$object\\$\\$index_para\\$, 0);",
                    AddAndGet
            ),

            GETANDADD_ZERO(
                    "(\\$type\\$) vh.getAndAdd(\\$object\\$\\$index_para\\$, 0);",
                    GetAndAdd
            ),

            GETANDSET_OUT(
                    "(\\$type\\$) vh.getAndSet(\\$object\\$\\$index_para\\$, \\$valueLiteral3\\$);",
                    GetAndSet
            ),

            COMPAREANDEXCHANGEVOLATILE(
                    "(\\$type\\$) vh.compareAndExchangeVolatile(\\$object\\$\\$index_para\\$, $1, $2);",
                    CompareAndExchangeVolatile
            ),

            COMPAREANDEXCHANGERELEASE(
                    "(\\$type\\$) vh.compareAndExchangeRelease(\\$object\\$\\$index_para\\$, $1, $2);",
                    CompareAndExchangeRelease
            ),

            COMPAREANDEXCHANGEACQUIRE(
                    "(\\$type\\$) vh.compareAndExchangeAcquire(\\$object\\$\\$index_para\\$, $1, $2);",
                    CompareAndExchangeAcquire
            ),

            COMPAREANDSET(
                    "vh.compareAndSet(\\$object\\$\\$index_para\\$, $1, $2);",
                    CompareAndSet
            ),

            WEAKCOMPAREANDSETRELEASE(
                    "vh.weakCompareAndSetRelease(\\$object\\$\\$index_para\\$, $1, $2);",
                    WeakCompareAndSetRelease
            ),

            WEAKCOMPAREANDSETACQUIRE(
                    "vh.weakCompareAndSetAcquire(\\$object\\$\\$index_para\\$, $1, $2);",
                    WeakCompareAndSetAcquire
            ),

            WEAKCOMPAREANDSET(
                    "vh.weakCompareAndSet(\\$object\\$\\$index_para\\$, $1, $2);",
                    WeakCompareAndSet
            ),

            WEAKCOMPAREANDSETVOLATILE(
                    "vh.weakCompareAndSetVolatile(\\$object\\$\\$index_para\\$, $1, $2);",
                    WeakCompareAndSetVolatile
            ),

            SET(
                    "vh.set(\\$object\\$\\$index_para\\$, $1);",
                    Set
            ),

            WEAKCOMPAREANDSET_SUC(
                    "vh.weakCompareAndSet(\\$object\\$\\$index_para\\$, \\$valueLiteral0\\$, $1);",
                    WeakCompareAndSet
            ),

            COMPAREANDEXCHANGEACQUIRE_SUC(
                    "vh.compareAndExchangeAcquire(\\$object\\$\\$index_para\\$, \\$valueLiteral0\\$, $1);",
                    CompareAndExchangeAcquire
            ),

            WEAKCOMPAREANDSETACQUIRE_SUC(
                    "vh.weakCompareAndSetAcquire(\\$object\\$\\$index_para\\$, \\$valueLiteral0\\$, $1);",
                    WeakCompareAndSetAcquire
            ),

            GET(
                    "(\\$type\\$) vh.get(\\$object\\$\\$index_para\\$);",
                    Get
            ),

            COMPAREANDEXCHANGERELEASE_FAIL(
                    "(\\$type\\$) vh.compareAndExchangeRelease(\\$object\\$\\$index_para\\$, \\$valueLiteral3\\$, \\$valueLiteral3\\$);",
                    CompareAndExchangeRelease
            ),

            WEAKCOMPAREANDSET_RETURN(
                    "vh.weakCompareAndSet(\\$object\\$\\$index_para\\$, \\$valueLiteral1\\$, \\$valueLiteral3\\$) ? \\$valueLiteral1\\$ : \\$valueLiteral0\\$;",
                    WeakCompareAndSet
            ),

            WEAKCOMPAREANDSETRELEASE_RETURN(
                    "vh.weakCompareAndSetRelease(\\$object\\$\\$index_para\\$, \\$valueLiteral1\\$, \\$valueLiteral3\\$) ? \\$valueLiteral1\\$ : \\$valueLiteral0\\$;",
                    WeakCompareAndSetRelease
            ),

            SETOPAQUE(
                    "vh.setOpaque(\\$object\\$\\$index_para\\$, $1);",
                    SetOpaque
            ),

            GETOPAQUE(
                    "(\\$type\\$) vh.getOpaque(\\$object\\$\\$index_para\\$);",
                    GetOpaque
            ),

            /* -------------------------------- fences -------------------------------- */

            GET_LOADLOADFENCE(
                    "%GetVar%;" + LNSEP + "VarHandle.loadLoadFence();",
                    LoadLoadFence
            ),

            STORESTOREFENCE_SET(
                    "VarHandle.storeStoreFence();" + LNSEP + "%SetVar<$1>%",
                    StoreStoreFence
            ),

            SET_STORESTOREFENCE(
                    "%SetVar<$1>%" + LNSEP + "VarHandle.storeStoreFence();",
                    StoreStoreFence
            ),

            GET_ACQUIREFENCE(
                    "%GetVar%;" + LNSEP + "VarHandle.acquireFence();",
                    AcquireFence
            ),

            RELEASEFENCE_SET(
                    "VarHandle.releaseFence();" + LNSEP + "%SetVar<$1>%",
                    ReleaseFence
            ),

            SET_RELEASEFENCE(
                    "%SetVar<$1>%" + LNSEP + "VarHandle.releaseFence();",
                    ReleaseFence
            ),

            SET_FULLFENCE(
                    "%SetVar<$1>%" + LNSEP + "VarHandle.fullFence();",
                    FullFence
            ),

            FULLFENCE_SET(
                    "VarHandle.fullFence();" + LNSEP + "%SetVar<$1>%",
                    FullFence
            ),

            GET_FULLFENCE(
                    "%GetVar%;" + LNSEP + "VarHandle.fullFence();",
                    FullFence
            ),

            FULLFENCE_GET(
                    "VarHandle.fullFence();" + LNSEP + "%GetVar%;",
                    FullFence
            ),

            ;

            Operation(String operation, Method method) {
                this.operation = operation;
                this.method = method;
            }

            String operation;
            Method method;
        }
    }

    private enum BufferType {
        ARRAY("", ""),
        HEAP("heap", "ByteBuffer.allocate"),
        DIRECT("direct", "ByteBuffer.allocateDirect");

        BufferType(String type, String allocateOp) {
            this.type = type;
            this.allocateOp = allocateOp;
        }

        String pkgAppendix() {
            return type.equals("") ? "" : "." + type;
        }

        private String type;
        String allocateOp;
    }

    private static final Map<String, Target> TEMPLATES = Map.ofEntries(
            entry("AddAndGetTest", T_ADDANDGET),
            entry("CAETest", T_CAE),
            entry("CASTest", T_CAS),
            entry("GetAndAddTest", T_GETANDADD),
            entry("GetAndSetTest", T_GETANDSET),
            entry("WeakCASTest", T_WEAKCAS),
            entry("WeakCASContendStrongTest", T_WEAKCAS),
            entry("LoadLoadFenceTest", T_GET_LOADLOADFENCE),
            entry("LoadStoreFenceTest1", T_LOADSTOREFENCE_SET),
            entry("LoadStoreFenceTest2", T_GET_LOADSTOREFENCE),
            entry("StoreLoadFenceTest", T_SET_STORELOADFENCE),
            entry("StoreStoreFenceTest1", T_STORESTOREFENCE_SET),
            entry("StoreStoreFenceTest2", T_SET_STORESTOREFENCE)
    );


    private static final String BASE_PKG = "org.openjdk.jcstress.tests.varHandles";

}
