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
package org.openjdk.jcstress.infra.processors;

import com.sun.source.tree.*;
import com.sun.source.util.Trees;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.runners.*;
import org.openjdk.jcstress.os.AffinitySupport;
import org.openjdk.jcstress.util.*;
import org.openjdk.jcstress.vm.AllocProfileSupport;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.*;

public class JCStressTestProcessor extends AbstractProcessor {

    private final List<TestInfo> tests = new ArrayList<>();

    public static final String TASK_LOOP_PREFIX = "task_";
    public static final String RUN_LOOP_PREFIX = "run_";
    public static final String AUX_PREFIX = "jcstress_";

    @Override
    public SourceVersion getSupportedSourceVersion() {
        // We may claim to support the latest version, since we are not using
        // any version-specific extensions.
        return SourceVersion.latest();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(JCStressTest.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            Set<? extends Element> set = roundEnv.getElementsAnnotatedWith(JCStressTest.class);
            for (Element el : set) {
                TypeElement e = (TypeElement) el;
                try {
                    TestInfo info = parseAndValidate(e);
                    Mode mode = el.getAnnotation(JCStressTest.class).value();
                    switch (mode) {
                        case Continuous:
                            generateContinuous(info);
                            break;
                        case Termination:
                            generateTermination(info);
                            break;
                        default:
                            throw new GenerationException("Unknown mode: " + mode, e);
                    }
                    tests.add(info);
                } catch (GenerationException ex) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ex.getMessage(), ex.getElement());
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } else {
            try {
                FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", TestList.LIST.substring(1));
                PrintWriter writer = new PrintWriter(file.openWriter());
                for (TestInfo test : tests) {
                    TestLineWriter wl = new TestLineWriter();

                    wl.put(test.getTest().getQualifiedName().toString());
                    wl.put(test.getGeneratedName());
                    wl.put(test.getDescription());
                    List<ExecutableElement> actors = test.getActors();
                    wl.put(actors.size());
                    for (ExecutableElement actor : actors) {
                        wl.put(actor.getSimpleName().toString());
                    }
                    wl.put(test.isRequiresFork());

                    wl.put(test.cases().size());

                    for (Outcome c : test.cases()) {
                        wl.put(c.expect().ordinal());
                        wl.put(c.desc());
                        wl.put(c.id().length);
                        for (String id : c.id()) {
                            wl.put(id);
                        }
                    }

                    wl.put(test.refs().size());
                    for (String ref : test.refs()) {
                        wl.put(ref);
                    }

                    writer.println(wl.get());
                }
                writer.close();
            } catch (IOException ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error writing MicroBenchmark list " + ex);
            }
        }
        return true;
    }

    private TestInfo parseAndValidate(TypeElement e) {
        TestInfo info = new TestInfo();

        info.setTest(e);

        // try to parse the external grading first
        String gradingName = JCStressMeta.class.getName();

        for (AnnotationMirror m : e.getAnnotationMirrors()) {
            if (gradingName.equals(m.getAnnotationType().toString())) {
                for(Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : m.getElementValues().entrySet()) {
                    if("value".equals(entry.getKey().getSimpleName().toString())) {
                        AnnotationValue value = entry.getValue();
                        parseMeta(processingEnv.getElementUtils().getTypeElement(value.getValue().toString()), info);
                        break;
                    }
                }
            }
        }

        // parse the metadata on the test itself
        parseMeta(e, info);

        for (ExecutableElement method : ElementFilter.methodsIn(e.getEnclosedElements())) {
            if (method.getAnnotation(Actor.class) != null) {
                info.addActor(method);
            }

            if (method.getAnnotation(Arbiter.class) != null) {
                info.setArbiter(method);
            }

            if (method.getAnnotation(Signal.class) != null) {
                info.setSignal(method);
            }

            if (method.getAnnotation(Actor.class) != null ||
                    method.getAnnotation(Arbiter.class) != null ||
                    method.getAnnotation(Signal.class) != null) {
                for (VariableElement var : method.getParameters()) {
                    TypeElement paramClass = (TypeElement) processingEnv.getTypeUtils().asElement(var.asType());
                    if (paramClass.getAnnotation(State.class) != null) {
                        info.setState(paramClass);
                    } else if (paramClass.getAnnotation(Result.class) != null) {
                        info.setResult(paramClass);
                    } else {
                        if (e.getAnnotation(JCStressTest.class).value() != Mode.Termination ||
                                !paramClass.getQualifiedName().toString().equals("java.lang.Thread")) {
                            throw new GenerationException("The parameter for @" + Actor.class.getSimpleName() +
                                    " methods requires either @" + State.class.getSimpleName() + " or @" + Result.class.getSimpleName() +
                                    " annotated class", var);
                        }
                    }
                }
            }
        }

        if (e.getAnnotation(State.class) != null) {
            info.setState(e);
        } else if (e.getAnnotation(Result.class) != null) {
            info.setResult(e);
        }

        String packageName = getPackageName(info.getTest());
        String testName = getGeneratedName(info.getTest());

        info.setGeneratedName(packageName + "." + testName);

        info.setRequiresFork(e.getAnnotation(JCStressTest.class).value() == Mode.Termination);

        return info;
    }

    private void parseMeta(TypeElement e, TestInfo info) {
        Outcome.Outcomes outcomes = e.getAnnotation(Outcome.Outcomes.class);
        if (outcomes != null) {
            for (Outcome c : outcomes.value()) {
                info.addCase(c);
            }
        }

        Outcome outcome = e.getAnnotation(Outcome.class);
        if (outcome != null) {
            info.addCase(outcome);
        }

        Ref.Refs refs = e.getAnnotation(Ref.Refs.class);
        if (refs != null) {
            for (Ref r : refs.value()) {
                info.addRef(r.value());
            }
        }

        Ref ref = e.getAnnotation(Ref.class);
        if (ref != null) {
            info.addRef(ref.value());
        }

        Description d = e.getAnnotation(Description.class);
        if (d != null) {
            info.setDescription(d.value());
        }
    }

    public static String getGeneratedName(Element ci) {
        String name = "";
        do {
            name = ci.getSimpleName() + (name.isEmpty() ? "" : "_" + name);
            ci = ci.getEnclosingElement();
        } while (ci != null && ci.getKind() != ElementKind.PACKAGE);
        return name + "_jcstress";
    }

    public static String getQualifiedName(Element ci) {
        String name = "";
        while (true) {
            Element parent = ci.getEnclosingElement();
            if (parent == null || parent.getKind() == ElementKind.PACKAGE) {
                name = ((TypeElement)ci).getQualifiedName() + (name.isEmpty() ? "" : "." + name);
                break;
            } else {
                name = ci.getSimpleName() + (name.isEmpty() ? "" : "." + name);
            }
            ci = parent;
        }
        return name;
    }

    private void generateContinuous(TestInfo info) {
        if (info.getState() == null) {
            throw new GenerationException("@" + JCStressTest.class.getSimpleName() + " defines no @" +
                    State.class.getSimpleName() + " to work with", info.getTest());
        }

        if (info.getResult() == null) {
            throw new GenerationException("@" + JCStressTest.class.getSimpleName() + " defines no @" +
                    Result.class.getSimpleName() + " to work with", info.getTest());
        }

        if (info.getState().getModifiers().contains(Modifier.FINAL)) {
            throw new GenerationException("@" + State.class.getSimpleName() + " should not be final.",
                    info.getState());
        }

        if (!info.getState().getModifiers().contains(Modifier.PUBLIC)) {
            throw new GenerationException("@" + State.class.getSimpleName() + " should be public.",
                    info.getState());
        }

        if (!info.getResult().getModifiers().contains(Modifier.PUBLIC)) {
            throw new GenerationException("@" + Result.class.getSimpleName() + " should be public.",
                    info.getResult());
        }

        if (!info.getResult().getSuperclass().toString().equals("java.lang.Object")) {
            throw new GenerationException("@" + Result.class.getSimpleName() + " should not inherit other classes.",
                    info.getResult());
        }

        String className = getGeneratedName(info.getTest());

        PrintWriter pw;
        Writer writer;
        try {
            writer = processingEnv.getFiler().createSourceFile(getPackageName(info.getTest()) + "." + className).openWriter();
            pw = new PrintWriter(writer);
        } catch (IOException e) {
            throw new GenerationException("IOException: " + e.getMessage(), info.getTest());
        }

        boolean isStateItself = info.getState().equals(info.getTest());

        String t = info.getTest().getSimpleName().toString();
        String s = info.getState().getSimpleName().toString();
        String r = info.getResult().getSimpleName().toString();


        int actorsCount = info.getActors().size();

        pw.println("package " + getPackageName(info.getTest()) + ";");

        printImports(pw, info);

        pw.println("public final class " + className + " extends Runner<" + r + "> {");
        pw.println();

        if (!isStateItself) {
            pw.println("    " + t + " test;");
        }
        pw.println("    volatile WorkerSync workerSync;");
        pw.println("    " + s + "[] gs;");
        pw.println("    " + r + "[] gr;");
        pw.println();

        pw.println("    public " + className + "(ForkedTestConfig config) {");
        pw.println("        super(config);");
        pw.println("    }");
        pw.println();

        pw.println("    @Override");
        pw.println("    public void sanityCheck(Counter<" + r + "> counter) throws Throwable {");
        pw.println("        sanityCheck_API(counter);");
        pw.println("        sanityCheck_Footprints(counter);");
        pw.println("    }");
        pw.println();
        pw.println("    private void sanityCheck_API(Counter<" + r + "> counter) throws Throwable {");

        pw.println("        final " + s + " s = new " + s + "();");
        pw.println("        final " + r + " r = new " + r + "();");
        if (!isStateItself) {
            pw.println("        final " + t + " t = new " + t + "();");
        }

        for (int a = 0; a < actorsCount; a++) {
            ExecutableElement el = info.getActors().get(a);
            pw.println("        VoidThread a" + a + " = new VoidThread() { protected void internalRun() {");
            pw.print("            ");
            emitMethod(pw, el, (isStateItself ? "s." : "t.") + el.getSimpleName(), "s", "r", false);
            pw.println(";");
            pw.println("        }};");
        }

        for (int a = 0; a < actorsCount; a++) {
            pw.println("        a" + a + ".start();");
        }

        for (int a = 0; a < actorsCount; a++) {
            pw.println("        a" + a + ".join();");
            pw.println("        if (a" + a + ".throwable() != null) {");
            pw.println("            throw a" + a + ".throwable();");
            pw.println("        }");
        }

        if (info.getArbiter() != null) {
            pw.print("            ");
            emitMethod(pw, info.getArbiter(), (isStateItself ? "s." : "t.") + info.getArbiter().getSimpleName(), "s", "r", false);
            pw.println(";");
        }
        pw.println("        counter.record(r);");
        pw.println("    }");
        pw.println();

        pw.println("    private void sanityCheck_Footprints(Counter<" + r + "> counter) throws Throwable {");
        pw.println("        config.adjustStrideCount(new FootprintEstimator() {");
        pw.println("          public void runWith(int size, long[] cnts) {");
        pw.println("            long time1 = System.nanoTime();");
        pw.println("            long alloc1 = AllocProfileSupport.getAllocatedBytes();");
        pw.println("            " + s + "[] ls = new " + s + "[size];");
        pw.println("            " + r + "[] lr = new " + r + "[size];");

        if (!isStateItself) {
            pw.println("            final " + t + " t = new " + t + "();");
        }

        pw.println("            for (int c = 0; c < size; c++) {");
        pw.println("                " + s + " s = new " + s + "();");
        pw.println("                " + r + " r = new " + r + "();");
        pw.println("                lr[c] = r;");
        pw.println("                ls[c] = s;");
        pw.println("            }");

        for (int a = 0; a < actorsCount; a++) {
            ExecutableElement el = info.getActors().get(a);
            pw.println("            LongThread a" + a + " = new LongThread() { public long internalRun() {");
            pw.println("                long a1 = AllocProfileSupport.getAllocatedBytes();");
            pw.println("                for (int c = 0; c < size; c++) {");
            pw.print("                    ");
            emitMethod(pw, el, (isStateItself ? "ls[c]." : "t.") + el.getSimpleName(), "ls[c]", "lr[c]", false);
            pw.println(";");
            pw.println("                }");
            pw.println("                long a2 = AllocProfileSupport.getAllocatedBytes();");
            pw.println("                return a2 - a1;");
            pw.println("            }};");
        }

        for (int a = 0; a < actorsCount; a++) {
            pw.println("            a" + a + ".start();");
        }

        for (int a = 0; a < actorsCount; a++) {
            pw.println("            try {");
            pw.println("                a" + a + ".join();");
            pw.println("                cnts[0] += a" + a + ".result();");
            pw.println("            } catch (InterruptedException e) {");
            pw.println("            }");
        }

        if (info.getArbiter() != null) {
            pw.println("            for (int c = 0; c < size; c++) {");
            pw.print("                ");
            emitMethod(pw, info.getArbiter(), (isStateItself ? "ls[c]." : "t.") + info.getArbiter().getSimpleName(), "ls[c]", "lr[c]", false);
            pw.println(";");
            pw.println("            }");
        }

        pw.println("            for (int c = 0; c < size; c++) {");
        pw.println("                counter.record(lr[c]);");
        pw.println("            }");
        pw.println("            long time2 = System.nanoTime();");
        pw.println("            long alloc2 = AllocProfileSupport.getAllocatedBytes();");
        pw.println("            cnts[0] += alloc2 - alloc1;");
        pw.println("            cnts[1] += time2 - time1;");
        pw.println("        }});");
        pw.println("    }");
        pw.println();

        pw.println("    @Override");
        pw.println("    public ArrayList<CounterThread<" + r + ">> internalRun() {");
        if (!isStateItself) {
            pw.println("        test = new " + t + "();");
        }
        pw.println("        int len = config.strideSize * config.strideCount;");
        pw.println("        gs = new " + s + "[len];");
        pw.println("        gr = new " + r + "[len];");
        pw.println("        for (int c = 0; c < len; c++) {");
        pw.println("            gs[c] = new " + s + "();");
        pw.println("            gr[c] = new " + r + "();");
        pw.println("        }");
        pw.println("        workerSync = new WorkerSync(false, " + actorsCount + ", config.spinLoopStyle);");
        pw.println();
        pw.println("        control.isStopped = false;");
        pw.println();

        // Initialize affinity before starting the timing measurement, so that init time
        // does not eat up into the test run time.
        pw.println("        if (config.localAffinity) {");
        pw.println("            try {");
        pw.println("                AffinitySupport.tryBind();");
        pw.println("            } catch (Exception e) {");
        pw.println("                // Do not care");
        pw.println("            }");
        pw.println("        }");
        pw.println();

        pw.println("        ArrayList<CounterThread<" + r + ">> threads = new ArrayList<>(" + actorsCount + ");");
        for (ExecutableElement a : info.getActors()) {
            pw.println("        threads.add(new CounterThread<" + r + ">() { public Counter<" + r + "> internalRun() {");
            pw.println("            return " + TASK_LOOP_PREFIX + a.getSimpleName() + "();");
            pw.println("        }});");
        }
        pw.println();
        pw.println("        for (CounterThread t : threads) {");
        pw.println("            t.start();");
        pw.println("        }");
        pw.println();
        pw.println("        if (config.time > 0) {");
        pw.println("            try {");
        pw.println("                TimeUnit.MILLISECONDS.sleep(config.time);");
        pw.println("            } catch (InterruptedException e) {");
        pw.println("            }");
        pw.println("        }");
        pw.println();
        pw.println("        control.isStopped = true;");
        pw.println();
        pw.println("        return threads;");
        pw.println("    }");
        pw.println();

        pw.println("    private void " + AUX_PREFIX + "consume(Counter<" + r + "> cnt, int a) {");
        pw.println("        " + s + "[] ls = gs;");
        pw.println("        " + r + "[] lr = gr;");
        pw.println("        int len = config.strideSize * config.strideCount;");
        pw.println("        int left = a * len / " + actorsCount + ";");
        pw.println("        int right = (a + 1) * len / " + actorsCount + ";");
        pw.println("        for (int c = left; c < right; c++) {");
        pw.println("            " + r + " r = lr[c];");
        pw.println("            " + s + " s = ls[c];");

        if (info.getArbiter() != null) {
            if (isStateItself) {
                emitMethod(pw, info.getArbiter(), "            s." + info.getArbiter().getSimpleName(), "s", "r", true);
            } else {
                emitMethod(pw, info.getArbiter(), "            test." + info.getArbiter().getSimpleName(), "s", "r", true);
            }
        }

        // If state is trivial, we can reset its fields directly, without
        // reallocating the object.

        if (allFieldsAreDefault(info.getState())) {
            for (VariableElement var : ElementFilter.fieldsIn(info.getState().getEnclosedElements())) {
                if (var.getModifiers().contains(Modifier.STATIC)) continue;
                pw.print("            s." + var.getSimpleName().toString() + " = ");
                pw.print(getDefaultVal(var));
                pw.println(";");
            }
        } else {
            pw.println("            ls[c] = new " + s + "();");
        }

        pw.println("            cnt.record(r);");

        for (VariableElement var : ElementFilter.fieldsIn(info.getResult().getEnclosedElements())) {
            if (var.getSimpleName().toString().equals("jcstress_trap")) continue;
            pw.print("            r." + var.getSimpleName().toString() + " = ");
            pw.print(getDefaultVal(var));
            pw.println(";");
        }

        pw.println("        }");
        pw.println("    }");
        pw.println();

        for (String type : new String[] { "int", "short", "byte", "char", "long", "float", "double", "Object" }) {
            pw.println("    private void " + AUX_PREFIX + "sink(" + type + " v) {};");
        }

        int n = 0;
        for (ExecutableElement a : info.getActors()) {
            pw.println();
            pw.println("    private Counter<" + r + "> " + TASK_LOOP_PREFIX + a.getSimpleName() + "() {");
            pw.println("        int len = config.strideSize * config.strideCount;");
            pw.println("        int stride = config.strideSize;");
            pw.println("        Counter<" + r + "> counter = new Counter<>();");
            pw.println("        if (config.localAffinity) AffinitySupport.bind(config.localAffinityMap[" + n + "]);");
            pw.println("        while (true) {");
            pw.println("            WorkerSync sync = workerSync;");
            pw.println("            if (sync.stopped) {");
            pw.println("                return counter;");
            pw.println("            }");
            pw.println("            int check = 0;");
            pw.println("            for (int start = 0; start < len; start += stride) {");
            pw.println("                " + RUN_LOOP_PREFIX + a.getSimpleName() + "(gs, gr, start, start + stride);");
            pw.println("                check += " + actorsCount + ";");
            pw.println("                sync.awaitCheckpoint(check);");
            pw.println("            }");
            pw.println("            " + AUX_PREFIX + "consume(counter, " + n + ");");
            pw.println("            if (sync.tryStartUpdate()) {");
            pw.println("                workerSync = new WorkerSync(control.isStopped, " + actorsCount + ", config.spinLoopStyle);");
            pw.println("            }");
            pw.println("            sync.postUpdate();");
            pw.println("        }");
            pw.println("    }");
            pw.println();
            pw.println("    private void " + RUN_LOOP_PREFIX + a.getSimpleName() + "(" + s + "[] gs, " + r + "[] gr, int start, int end) {");
            if (!isStateItself) {
                pw.println("        " + t + " lt = test;");
            }
            pw.println("        " + s + "[] ls = gs;");
            pw.println("        " + r + "[] lr = gr;");
            pw.println("        for (int c = start; c < end; c++) {");

            // Try to access both state and result fields early. This will help
            // compiler to avoid null-pointer checks in the workload, which will
            // free it to choose alternative load/store orders.
            //
            // For results, we access the most convenient result field, and make sure
            // its null-checking effects stays behind by calling the empty method.
            // That method would be normally inlined and eliminated, but the NP-check
            // would persist.
            //
            // For states that are passed as arguments we can do the same.
            // For states that are receivers themselves, we already have the NP-check.

            pw.println("            " + s + " s = ls[c];");
            if (hasResultArgs(a)) {
                pw.println("            " + r + " r = lr[c];");
                pw.println("            " + AUX_PREFIX + "sink(r.jcstress_trap);");
            }

            if (isStateItself) {
                emitMethod(pw, a, "            s." + a.getSimpleName(), "s", "r", true);
            } else {
                String sf = selectSinkField(info.getState());
                if (sf != null) {
                    pw.println("            " + AUX_PREFIX + "sink(s." + sf + ");");
                }
                emitMethod(pw, a, "           lt." + a.getSimpleName(), "s", "r", true);
            }

            pw.println("        }");
            pw.println("    }");
            n++;
        }

        pw.println();
        pw.println("}");

        pw.close();
    }

    private String selectSinkField(TypeElement cl) {
        String[] typePref = { "int", "short", "byte", "char", "long", "float", "double" };

        // Select first field of preferential type
        for (String typeP : typePref) {
            for (VariableElement var : ElementFilter.fieldsIn(cl.getEnclosedElements())) {
                Set<Modifier> mods = var.getModifiers();
                if (mods.contains(Modifier.STATIC)) continue;
                if (mods.contains(Modifier.PRIVATE)) continue;

                String t = var.asType().toString();
                if (t.equals(typeP)) return var.getSimpleName().toString();
            }
        }

        // Return first non-preferenced, e.g. Object subclass
        for (VariableElement var : ElementFilter.fieldsIn(cl.getEnclosedElements())) {
            Set<Modifier> mods = var.getModifiers();
            if (mods.contains(Modifier.STATIC)) continue;
            if (mods.contains(Modifier.PRIVATE)) continue;

            return var.getSimpleName().toString();
        }

        return null;
    }

    /**
     * @param el to check
     * @return true, if all instance fields are initialized to default values
     */
    private boolean allFieldsAreDefault(TypeElement el) {
        // No fields in superclasses
        if (!el.getSuperclass().toString().equals("java.lang.Object")) {
            return false;
        }
        for (VariableElement v : ElementFilter.fieldsIn(el.getEnclosedElements())) {
            Set<Modifier> mods = v.getModifiers();

            // Bypass static fields, these do not affect instances
            if (mods.contains(Modifier.STATIC)) continue;

            // No final, private, or protected fields
            if (mods.contains(Modifier.FINAL)) return false;
            if (mods.contains(Modifier.PRIVATE)) return false;
            if (mods.contains(Modifier.PROTECTED)) return false;
        }

        Trees trees = Trees.instance(processingEnv);
        ClassTree tree = trees.getTree(el);

        if (tree == null) {
            // Assume the worst.
            return false;
        }

        for (Tree member : tree.getMembers()) {
            if (member.getKind() == Tree.Kind.METHOD) {
                MethodTree m = (MethodTree) member;
                if (m.getName().toString().equals("<init>")) {
                    BlockTree body = m.getBody();
                    List<? extends StatementTree> b = body.getStatements();

                    // no non-trivial constructors
                    if (b.size() != 1) return false;
                    if (!b.get(0).toString().equals("super();")) return false;
                }
            }
            if (member.getKind() == Tree.Kind.VARIABLE) {
                VariableTree t = (VariableTree) member;

                // no field initializers of any kind
                if (t.getInitializer() != null) return false;
            }
            if (member.getKind() == Tree.Kind.BLOCK) {
                BlockTree b = (BlockTree) member;
                // no instance initializers of any kind
                if (!b.isStatic()) return false;
            }
        }
        return true;
    }

    private String getDefaultVal(VariableElement var) {
        String type = var.asType().toString();
        String val;
        switch (type) {
            case "int":
            case "long":
            case "short":
            case "byte":
            case "char":
                val = "0";
                break;
            case "double":
                val = "0D";
                break;
            case "float":
                val = "0F";
                break;
            case "boolean":
                val = "false";
                break;
            default:
                val = "null";
        }
        return val;
    }

    private void generateTermination(TestInfo info) {
        if (info.getSignal() == null) {
            throw new GenerationException("@" + JCStressTest.class.getSimpleName() + " with mode=" + Mode.Termination +
                    " should have a @" + Signal.class.getSimpleName() + " method", info.getTest());
        }

        if (info.getActors().size() != 1) {
            throw new GenerationException("@" + JCStressTest.class.getSimpleName() + " with mode=" + Mode.Termination +
                    " should have only the single @" + Actor.class.getName(), info.getTest());
        }

        String generatedName = getGeneratedName(info.getTest());

        PrintWriter pw;
        Writer writer;
        try {
            writer = processingEnv.getFiler().createSourceFile(getPackageName(info.getTest()) + "." + generatedName).openWriter();
            pw = new PrintWriter(writer);
        } catch (IOException e) {
            throw new GenerationException("IOException: " + e.getMessage(), info.getTest());
        }

        String t = info.getTest().getSimpleName().toString();

        ExecutableElement actor = info.getActors().get(0);

        pw.println("package " + getPackageName(info.getTest()) + ";");

        printImports(pw, info);

        pw.println("public class " + generatedName + " extends Runner<" + generatedName + ".Outcome> {");
        pw.println();

        pw.println("    public " + generatedName + "(ForkedTestConfig config) {");
        pw.println("        super(config);");
        pw.println("    }");
        pw.println();

        pw.println("    @Override");
        pw.println("    public TestResult run() {");
        pw.println("        Counter<Outcome> results = new Counter<>();");
        pw.println();
        pw.println("        for (int c = 0; c < config.iters; c++) {");
        pw.println("            run(results);");
        pw.println();
        pw.println("            if (results.count(Outcome.STALE) > 0) {");
        pw.println("                forceExit = true;");
        pw.println("                break;");
        pw.println("            }");
        pw.println("        }");
        pw.println();
        pw.println("        return dump(results);");
        pw.println("    }");
        pw.println();
        pw.println("    @Override");
        pw.println("    public void sanityCheck(Counter<Outcome> counter) throws Throwable {");
        pw.println("        throw new UnsupportedOperationException();");
        pw.println("    }");
        pw.println();
        pw.println("    @Override");
        pw.println("    public ArrayList<CounterThread<Outcome>> internalRun() {");
        pw.println("        throw new UnsupportedOperationException();");
        pw.println("    }");
        pw.println();
        pw.println("    private void run(Counter<Outcome> results) {");
        pw.println("        long target = System.currentTimeMillis() + config.time;");
        pw.println("        while (System.currentTimeMillis() < target) {");
        pw.println();

        if (info.getTest().equals(info.getState())) {
            pw.println("            final " + info.getState().getSimpleName() + " state = new " + info.getState().getSimpleName() + "();");
        } else {
            if (info.getState() != null) {
                pw.println("            final " + info.getState().getSimpleName() + " state = new " + info.getState().getSimpleName() + "();");
            }
            pw.println("            final " + t + " test = new " + t + "();");
        }

        pw.println("            final Holder holder = new Holder();");
        pw.println();
        pw.println("            Thread t1 = new Thread(new Runnable() {");
        pw.println("                public void run() {");
        pw.println("                    try {");
        pw.println("                        holder.started = true;");

        if (info.getTest().equals(info.getState())) {
            emitMethodTermination(pw, actor, "                        state." + actor.getSimpleName(), "state");
        } else {
            emitMethodTermination(pw, actor, "                        test." + actor.getSimpleName(), "state");
        }

        pw.println("                    } catch (Exception e) {");
        pw.println("                        holder.error = true;");
        pw.println("                    }");
        pw.println("                    holder.terminated = true;");
        pw.println("                }");
        pw.println("            });");
        pw.println("            t1.setDaemon(true);");
        pw.println("            t1.start();");
        pw.println();
        pw.println("            while (!holder.started) {");
        pw.println("                try {");
        pw.println("                    TimeUnit.MILLISECONDS.sleep(1);");
        pw.println("                } catch (InterruptedException e) {");
        pw.println("                    // do nothing");
        pw.println("                }");
        pw.println("            }");
        pw.println();
        pw.println("            try {");

        if (info.getTest().equals(info.getState())) {
            emitMethodTermination(pw, info.getSignal(), "                state." + info.getSignal().getSimpleName(), "state");
        } else {
            emitMethodTermination(pw, info.getSignal(), "                test." + info.getSignal().getSimpleName(), "state");
        }

        pw.println("            } catch (Exception e) {");
        pw.println("                holder.error = true;");
        pw.println("            }");
        pw.println();
        pw.println("            try {");
        pw.println("                t1.join(Math.max(2*config.time, Runner.MIN_TIMEOUT_MS));");
        pw.println("            } catch (InterruptedException e) {");
        pw.println("                // do nothing");
        pw.println("            }");
        pw.println();
        pw.println("            if (holder.terminated) {");
        pw.println("                if (holder.error) {");
        pw.println("                    results.record(Outcome.ERROR);");
        pw.println("                } else {");
        pw.println("                    results.record(Outcome.TERMINATED);");
        pw.println("                }");
        pw.println("            } else {");
        pw.println("                results.record(Outcome.STALE);");
        pw.println("                return;");
        pw.println("            }");
        pw.println("        }");
        pw.println("    }");
        pw.println();
        pw.println("    private static class Holder {");
        pw.println("        volatile boolean started;");
        pw.println("        volatile boolean terminated;");
        pw.println("        volatile boolean error;");
        pw.println("    }");
        pw.println();
        pw.println("    public enum Outcome {");
        pw.println("        TERMINATED,");
        pw.println("        STALE,");
        pw.println("        ERROR,");
        pw.println("    }");
        pw.println("}");
        pw.close();
    }

    private boolean hasResultArgs(ExecutableElement el) {
        for (VariableElement var : el.getParameters()) {
            TypeElement paramClass = (TypeElement) processingEnv.getTypeUtils().asElement(var.asType());
            if (paramClass.getAnnotation(Result.class) != null) {
                return true;
            }
        }
        return false;
    }

    private void emitMethod(PrintWriter pw, ExecutableElement el, String lvalue, String stateAccessor, String resultAccessor, boolean terminate) {
        pw.print(lvalue + "(");

        boolean isFirst = true;
        for (VariableElement var : el.getParameters()) {
            if (isFirst) {
                isFirst = false;
            } else {
                pw.print(", ");
            }
            TypeElement paramClass = (TypeElement) processingEnv.getTypeUtils().asElement(var.asType());
            if (paramClass.getAnnotation(State.class) != null) {
                pw.print(stateAccessor);
            } else if (paramClass.getAnnotation(Result.class) != null) {
                pw.print(resultAccessor);
            }
        }
        pw.print(")");
        if (terminate) {
            pw.println(";");
        }
    }

    private void emitMethodTermination(PrintWriter pw, ExecutableElement el, String lvalue, String stateAccessor) {
        pw.print(lvalue + "(");

        boolean isFirst = true;
        for (VariableElement var : el.getParameters()) {
            if (isFirst) {
                isFirst = false;
            } else {
                pw.print(", ");
            }
            TypeElement paramClass = (TypeElement) processingEnv.getTypeUtils().asElement(var.asType());
            if (paramClass.getAnnotation(State.class) != null) {
                pw.print(stateAccessor);
            }
            if (paramClass.getQualifiedName().toString().equals("java.lang.Thread")) {
                pw.print("t1");
            }
        }
        pw.println(");");
    }

    private void printImports(PrintWriter pw, TestInfo info) {
        Class<?>[] imports = new Class<?>[] {
                ArrayList.class, Arrays.class,
                TimeUnit.class,
                ForkedTestConfig.class, TestResult.class,
                Runner.class, WorkerSync.class, Counter.class,
                AffinitySupport.class, AllocProfileSupport.class,
                FootprintEstimator.class,
                VoidThread.class, LongThread.class, CounterThread.class
        };

        for (Class<?> c : imports) {
            pw.println("import " + c.getName() + ';');
        }
        pw.println("import " + info.getTest().getQualifiedName() + ";");
        if (info.getResult() != null) {
            pw.println("import " + info.getResult().getQualifiedName() + ";");
        }
        if (!info.getTest().equals(info.getState())) {
            if (info.getState() != null) {
                pw.println("import " + info.getState().getQualifiedName() + ";");
            }
        }

        pw.println();
    }

    public String getPackageName(Element el) {
        Element walk = el;
        while (walk.getKind() != ElementKind.PACKAGE) {
            walk = walk.getEnclosingElement();
        }
        return ((PackageElement)walk).getQualifiedName().toString();
    }

}
