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
package org.openjdk.jcstress.generator;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class TestGenerator {

    private final String srcRoot;
    private final String resRoot;

    private final ResultGenerator resultGenerator;
    private PrintWriter resourceWriter;

    public TestGenerator(String srcRoot, String resRoot) {
        this.srcRoot = srcRoot;
        this.resRoot = resRoot;
        this.resultGenerator = new ResultGenerator(srcRoot);
    }

    public void run() throws FileNotFoundException {
        generateMemoryEffects();
    }

    public void generateMemoryEffects() throws FileNotFoundException {
        resourceWriter = new PrintWriter(Utils.ensureDir(resRoot + "/org/openjdk/jcstress/desc/") + "/memeffects.xml");
        resourceWriter.println("<testsuite>");

        for (Class<?> varType : Types.SUPPORTED_PRIMITIVES) {
            for (Class<?> guardType : Types.SUPPORTED_PRIMITIVES) {
                generate(new Types(guardType, varType), new VolatileReadWrite(guardType), "volatile_" + guardType + "_" + varType, "org.openjdk.jcstress.tests.memeffects.basic.volatiles");
            }
            generate(new Types(int.class, varType), new SynchronizedBlock(), "lock_" + varType, "org.openjdk.jcstress.tests.memeffects.basic.lock");

            for (Class<?> guardType : Types.SUPPORTED_ATOMICS) {
                Class<?> primType = Types.mapAtomicToPrim(guardType);
                for (AcqType acqType : AcqType.values()) {
                    for (RelType relType : RelType.values()) {
                        try {
                            generate(
                                new Types(primType, varType),
                                new Atomic_X(guardType, primType, acqType, relType),
                                "atomic_" + acqType + "_" + relType + "_" + varType,
                                "org.openjdk.jcstress.tests.memeffects.basic.atomic." + guardType.getSimpleName());
                        } catch (IllegalArgumentException iae) {
                            // not compatible acq/rel types, move on.
                        }
                    }
                }
            }

            for (Class<?> guardType : Types.SUPPORTED_ATOMIC_UPDATERS) {
                Class<?> primType = Types.mapAtomicToPrim(guardType);
                for (AcqType acqType : AcqType.values()) {
                    for (RelType relType : RelType.values()) {
                        try {
                            generate(
                                new Types(primType, varType),
                                new Atomic_Updater_X(guardType, primType, acqType, relType),
                                "atomic_" + acqType + "_" + relType + "_" + varType,
                                "org.openjdk.jcstress.tests.memeffects.basic.atomicupdaters." + guardType.getSimpleName());
                        } catch (IllegalArgumentException iae) {
                            // not compatible acq/rel types, move on.
                        }
                    }
                }
            }

        }

        resourceWriter.println("</testsuite>");
        resourceWriter.close();
    }

   public void generate(Types types, Primitive prim, String klass, String pkg) throws FileNotFoundException {

        resourceWriter.println("    <test name=\"" + pkg + "." + klass + "\">\n" +
                "        <contributed-by>Aleksey Shipilev (aleksey.shipilev@oracle.com)</contributed-by>\n" +
                "        <description>Generated test</description>\n" +
                "        <case>\n" +
                "            <match>[" + getDefaultValue(types.type(0)) +", " + getDefaultValue(types.type(1))+ "]</match>\n" +
                "            <match>[" + getDefaultValue(types.type(0)) +", " + getSetValue(types.type(1))+ "]</match>\n" +
                "            <expect>ACCEPTABLE</expect>\n" +
                "            <description>Seeing default guard, can see any value</description>\n" +
                "        </case>\n" +
                "        <case>\n" +
                "            <match>[" + getSetValue(types.type(0)) +", " + getSetValue(types.type(1))+ "]</match>\n" +
                "            <expect>ACCEPTABLE</expect>\n" +
                "            <description>Seeing set guard, seeing the updated value</description>\n" +
                "        </case>\n" +
                "        <case>\n" +
                "            <match>[" + getSetValue(types.type(0)) +", " + getDefaultValue(types.type(1))+ "]</match>\n" +
                "            <expect>FORBIDDEN</expect>\n" +
                "            <description>Seeing set guard, not seeing the updated value</description>\n" +
                "        </case>\n" +
                "        <unmatched>\n" +
                "            <expect>FORBIDDEN</expect>\n" +
                "            <description>Other cases are not expected.</description>\n" +
                "        </unmatched>\n" +
                "    </test>");

        String resultName = resultGenerator.generateResult(types);

        String pathname = Utils.ensureDir(srcRoot + "/" + pkg.replaceAll("\\.", "/"));

        PrintWriter pw = new PrintWriter(pathname + "/" + klass + ".java");

        pw.println("package " + pkg +";\n" +
                "\n" +
                "import java.util.concurrent.*;\n" +
                "import java.util.concurrent.atomic.*;\n" +
                "import org.openjdk.jcstress.infra.results." + resultName + ";\n" +
                "import org.openjdk.jcstress.tests.Actor2_Test;\n" +
                "\n" +
                "public class " + klass + " implements Actor2_Test<" + klass + ".State, " + resultName + "> {\n" +
                "\n" +
                "    @Override\n" +
                "    public State newState() {\n" +
                "        return new State();\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void actor1(State s, " + resultName +" r) {");

        pw.println("        " + prim.printRelease("        s.a = " + getRValue(types.type(1)) +";"));

        pw.println(
                "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public void actor2(State s, " + resultName +" r) {");

        pw.println("        " + prim.printAcquire("        r.r2 = s.a;"));

        pw.println(
                "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public " + resultName + " newResult() {\n" +
                        "        return new " + resultName + "();\n" +
                        "    }\n" +
                        "\n" +
                        "    public static class State {");

        pw.println("        " + prim.printStateField());

        pw.println("        public " + types.type(1) + " a;");
        pw.println("    }");
        pw.println("}");

        pw.close();
    }

    public static String getDefaultValue(Class<?> k) {
        if (k == boolean.class) return "false";
        if (k == byte.class)    return "0";
        if (k == short.class)   return "0";
        if (k == char.class)    return "0";
        if (k == int.class)     return "0";
        if (k == long.class)    return "0";
        if (k == float.class)   return "0.0";
        if (k == double.class)  return "0.0";
        return null;
    }

    public static String getSetValue(Class<?> k) {
        if (k == boolean.class) return "true";
        if (k == byte.class)    return "1";
        if (k == short.class)   return "42";
        if (k == char.class)    return "65";
        if (k == int.class)     return "42";
        if (k == long.class)    return "42";
        if (k == float.class)   return "42.0";
        if (k == double.class)  return "42.0";
        return null;
    }

    public static String getRValue(Class<?> k) {
        if (k == boolean.class) return "true";
        if (k == byte.class)    return "(byte)1";
        if (k == short.class)   return "42";
        if (k == char.class)    return "'A'";
        if (k == int.class)     return "42";
        if (k == long.class)    return "42L";
        if (k == float.class)   return "42.0f";
        if (k == double.class)  return "42.0d";
        return null;
    }

    public static String getUnitValue(Class<?> k) {
        if (k == boolean.class) return "false";
        if (k == byte.class)    return "1";
        if (k == short.class)   return "1";
        if (k == char.class)    return "(char)1";
        if (k == int.class)     return "1";
        if (k == long.class)    return "1L";
        if (k == float.class)   return "1.0f";
        if (k == double.class)  return "1.0d";
        return null;
    }

    public static class Types {
        public static final Class<?>[] SUPPORTED_PRIMITIVES =
                new Class<?>[] { boolean.class, byte.class, short.class, char.class,
                                 int.class, long.class, float.class, double.class};

        public static final Class<?>[] SUPPORTED_ATOMICS =
                new Class<?>[] { AtomicInteger.class, AtomicLong.class, AtomicBoolean.class };

        public static final Class<?>[] SUPPORTED_ATOMIC_UPDATERS =
                new Class<?>[] { AtomicIntegerFieldUpdater.class, AtomicLongFieldUpdater.class };

        private final Class<?>[] types;

        public Types(Class<?>... types) {
            this.types = types;
        }

        public Class<?> type(int index) {
            return types[index];
        }

        public Class[] all() {
            return types;
        }

        public static Class<?> mapAtomicToPrim(Class<?> guardType) {
            if (guardType == AtomicInteger.class) return int.class;
            if (guardType == AtomicLong.class) return long.class;
            if (guardType == AtomicBoolean.class) return boolean.class;
            if (guardType == AtomicIntegerFieldUpdater.class) return int.class;
            if (guardType == AtomicLongFieldUpdater.class) return long.class;
            throw new IllegalStateException("No case");
        }
    }

}
