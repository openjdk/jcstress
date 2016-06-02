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
import org.openjdk.jcstress.infra.collectors.TestResultCollector;
import org.openjdk.jcstress.infra.runners.*;
import org.openjdk.jcstress.util.*;
import org.openjdk.jcstress.vm.WhiteBoxSupport;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
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
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;


@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class JCStressTestProcessor extends AbstractProcessor {

    private final List<TestInfo> tests = new ArrayList<>();

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

                    wl.put(test.getTest().getQualifiedName());
                    wl.put(test.getGeneratedName());
                    wl.put(test.getDescription());
                    wl.put(test.getActors().size());
                    wl.put(test.isRequiresFork());

                    wl.put(test.cases().size());

                    for (Outcome c : test.cases()) {
                        wl.put(c.expect());
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

        if (info.getResult().getModifiers().contains(Modifier.FINAL)) {
            throw new GenerationException("@" + Result.class.getSimpleName() + " should not be final.",
                    info.getResult());
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
        String s = isStateItself ?
                        info.getState().getSimpleName().toString() :
                        getGeneratedName(info.getState());
        String r = getGeneratedName(info.getResult());

        generateTrapSubclass(info.getResult());
        if (!isStateItself) {
            generateTrapSubclass(info.getState());
        }

        int actorsCount = info.getActors().size();

        pw.println("package " + getPackageName(info.getTest()) + ";");

        printImports(pw, info);

        pw.println("public class " + className + " extends Runner<" + r + "> {");
        pw.println();

        for (ExecutableElement a : info.getActors()) {
            pw.println("    OpenAddressHashCounter<" + r + "> counter_" + a.getSimpleName() + ";");
        }

        if (!isStateItself) {
            pw.println("    " + t + " test;");
        }
        pw.println("    volatile StateHolder<Pair> version;");
        pw.println();

        pw.println("    public " + className + "(TestConfig config, TestResultCollector collector, ExecutorService pool) {");
        pw.println("        super(config, collector, pool, \"" + getQualifiedName(info.getTest()) + "\");");
        pw.println("    }");
        pw.println();

        pw.println("    @Override");
        pw.println("    public void sanityCheck() throws Throwable {");
        pw.println("        final " + t + " t = new " + t + "();");
        pw.println("        final " + s + " s = new " + s + "();");
        pw.println("        final " + r + " r = new " + r + "();");

        pw.println("        Collection<Future<?>> res = new ArrayList<>();");
        for (ExecutableElement el : info.getActors()) {
            pw.print("        res.add(pool.submit(() -> ");
            emitMethod(pw, el, "t." + el.getSimpleName(), "s", "r", false);
            pw.println("));");
        }

        pw.println("        for (Future<?> f : res) {");
        pw.println("            try {");
        pw.println("                f.get();");
        pw.println("            } catch (ExecutionException e) {");
        pw.println("                throw e.getCause();");
        pw.println("            }");
        pw.println("        }");

        if (info.getArbiter() != null) {
            emitMethod(pw, info.getArbiter(), "        t." + info.getArbiter().getSimpleName(), "s", "r", true);
        }

        pw.println("    }");
        pw.println();

        pw.println("    @Override");
        pw.println("    public Counter<" + r + "> internalRun() {");
        if (!isStateItself) {
            pw.println("        test = new " + t + "();");
        }
        pw.println("        version = new StateHolder<>(new Pair[0], " + actorsCount + ", config.spinLoopStyle);");

        for (ExecutableElement a : info.getActors()) {
            pw.println("        counter_" + a.getSimpleName() + " = new OpenAddressHashCounter<>();");
        }


        pw.println();
        pw.println("        control.isStopped = false;");
        pw.println("        Collection<Future<?>> tasks = new ArrayList<>();");

        for (ExecutableElement a : info.getActors()) {
            pw.println("        tasks.add(pool.submit(this::" + a.getSimpleName() + "));");
        }

        pw.println();
        pw.println("        try {");
        pw.println("            TimeUnit.MILLISECONDS.sleep(config.time);");
        pw.println("        } catch (InterruptedException e) {");
        pw.println("        }");
        pw.println();
        pw.println("        control.isStopped = true;");
        pw.println();
        pw.println("        waitFor(tasks);");
        pw.println();
        pw.println("        Counter<" + r + "> counter = new OpenAddressHashCounter<>();");
        for (ExecutableElement a : info.getActors()) {
            pw.println("        counter.merge(counter_" + a.getSimpleName() + ");");
        }
        pw.println("        return counter;");
        pw.println("    }");
        pw.println();

        pw.println("    public final void jcstress_consume(StateHolder<Pair> holder, Counter<" + r + "> cnt, int a, int actors) {");
        pw.println("        Pair[] pairs = holder.pairs;");
        pw.println("        int len = pairs.length;");
        pw.println("        int left = a * len / actors;");
        pw.println("        int right = (a + 1) * len / actors;");
        pw.println("        for (int c = left; c < right; c++) {");
        pw.println("            Pair p = pairs[c];");
        pw.println("            " + r + " r = p.r;");
        pw.println("            " + s + " s = p.s;");

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
            pw.println("            p.s = new " + s + "();");
        }

        pw.println("            cnt.record(r);");

        for (VariableElement var : ElementFilter.fieldsIn(info.getResult().getEnclosedElements())) {
            pw.print("            r." + var.getSimpleName().toString() + " = ");
            pw.print(getDefaultVal(var));
            pw.println(";");
        }

        pw.println("        }");
        pw.println("    }");
        pw.println();

        pw.println("    public final void jcstress_updateHolder(StateHolder<Pair> holder) {");
        pw.println("        if (!holder.tryStartUpdate()) return;");
        pw.println("        Pair[] pairs = holder.pairs;");
        pw.println("        int len = pairs.length;");
        pw.println();
        pw.println("        int newLen = holder.updateStride ? Math.max(config.minStride, Math.min(len * 2, config.maxStride)) : len;");
        pw.println();
        pw.println("        Pair[] newPairs = pairs;");
        pw.println("        if (newLen > len) {");
        pw.println("            newPairs = Arrays.copyOf(pairs, newLen);");
        pw.println("            for (int c = len; c < newLen; c++) {");
        pw.println("                Pair p = new Pair();");
        pw.println("                p.r = new " + r + "();");
        pw.println("                p.s = new " + s + "();");
        pw.println("                newPairs[c] = p;");
        pw.println("            }");
        pw.println("         }");
        pw.println();
        pw.println("        version = new StateHolder<>(control.isStopped, newPairs, " + actorsCount + ", config.spinLoopStyle);");
        pw.println("        holder.finishUpdate();");
        pw.println("   }");

        int n = 0;
        for (ExecutableElement a : info.getActors()) {
            pw.println();
            pw.println("    public final Void " + a.getSimpleName() + "() {");
            pw.println();
            if (!isStateItself) {
                pw.println("        " + t + " lt = test;");
            }
            pw.println("        while (true) {");
            pw.println("            StateHolder<Pair> holder = version;");
            pw.println("            if (holder.stopped) {");
            pw.println("                return null;");
            pw.println("            }");
            pw.println();
            pw.println("            Pair[] pairs = holder.pairs;");
            pw.println();
            pw.println("            holder.preRun();");
            pw.println();
            pw.println("            for (Pair p : pairs) {");

            // Try to access both state and result fields early. This will help
            // compiler to avoid null-pointer checks in the workload, which will
            // free it to choose alternative load/store orders.
            //
            // For results, we can touch the synthetic "trap" field.
            // For states that are passed as arguments we can do the same.
            // For states that are receivers themselves, we already have the NP-check.

            if (hasResultArgs(a)) {
                pw.println("                " + r + " r = p.r;");
                pw.println("                r.trap = 0;");
            }

            if (isStateItself) {
                emitMethod(pw, a, "                p.s." + a.getSimpleName(), "p.s", "r", true);
            } else {
                pw.println("                " + s + " s = p.s;");
                pw.println("                s.trap = 0;");
                emitMethod(pw, a, "                lt." + a.getSimpleName(), "s", "r", true);
            }

            pw.println("            }");
            pw.println();
            pw.println("            holder.postRun();");
            pw.println();
            pw.println("            jcstress_consume(holder, counter_" + a.getSimpleName() + ", " + n + ", " + actorsCount + ");");
            pw.println("            jcstress_updateHolder(holder);");
            pw.println();
            pw.println("            holder.postUpdate();");
            pw.println("        }");
            pw.println("    }");
            n++;
        }

        pw.println();
        pw.println("    static class Pair {");
        pw.println("        public " + s + " s;");
        pw.println("        public " + r + " r;");
        pw.println("    }");
        pw.println("}");

        pw.close();
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
                // no instance initializers of any kind
                return false;
            }
        }
        return true;
    }

    private void generateTrapSubclass(TypeElement el) {
        String name = getGeneratedName(el);

        PrintWriter pw;
        try {
            Writer writer = processingEnv.getFiler().createSourceFile(getPackageName(el) + "." + name).openWriter();
            pw = new PrintWriter(writer);
        } catch (IOException e) {
            // may happen when file is already generated
            return;
        }

        pw.println("package " + getPackageName(el) + ";");
        pw.println("public class " + name + " extends " + el.getQualifiedName() + "{ ");
        pw.println("    @sun.misc.Contended");
        pw.println("    @jdk.internal.vm.annotation.Contended");
        pw.println("    public int trap;");
        pw.println("}");

        pw.close();
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

        pw.println("    public " + generatedName + "(TestConfig config, TestResultCollector collector, ExecutorService pool) {");
        pw.println("        super(config, collector, pool, \"" + getQualifiedName(info.getTest()) + "\");");
        pw.println("    }");
        pw.println();

        pw.println("    @Override");
        pw.println("    public void run() {");
        pw.println("        testLog.println(\"Running \" + testName);");
        pw.println();
        pw.println("        Counter<Outcome> results = new OpenAddressHashCounter<>();");
        pw.println();
        pw.println("        testLog.print(\"Iterations \");");
        pw.println("        for (int c = 0; c < config.iters; c++) {");
        pw.println("            try {");
        pw.println("                WhiteBoxSupport.tryDeopt(config.deoptRatio);");
        pw.println("            } catch (NoClassDefFoundError err) {");
        pw.println("                // gracefully \"handle\"");
        pw.println("            }");
        pw.println();
        pw.println("            testLog.print(\".\");");
        pw.println("            testLog.flush();");
        pw.println("            run(results);");
        pw.println();
        pw.println("            dump(c, results);");
        pw.println();
        pw.println("            if (results.count(Outcome.STALE) > 0) {");
        pw.println("                testLog.println(\"Have stale threads, forcing VM to exit\");");
        pw.println("                testLog.flush();");
        pw.println("                testLog.close();");
        pw.println("                System.exit(0);");
        pw.println("            }");
        pw.println("        }");
        pw.println("        testLog.println();");
        pw.println("    }");
        pw.println();
        pw.println("    @Override");
        pw.println("    public void sanityCheck() throws Throwable {");
        pw.println("        throw new UnsupportedOperationException();");
        pw.println("    }");
        pw.println();
        pw.println("    @Override");
        pw.println("    public Counter<Outcome> internalRun() {");
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
        pw.println("            t1.start();");
        pw.println();
        pw.println("            try {");
        pw.println("                TimeUnit.MILLISECONDS.sleep(10);");
        pw.println("            } catch (InterruptedException e) {");
        pw.println("                // do nothing");
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
        pw.println("                t1.join(1000);");
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
                ArrayList.class, Arrays.class, Collection.class,
                ExecutorService.class, Future.class, TimeUnit.class,
                TestConfig.class, TestResultCollector.class,
                Runner.class, StateHolder.class, Counter.class,
                WhiteBoxSupport.class, OpenAddressHashCounter.class, ExecutionException.class
        };

        for (Class<?> c : imports) {
            pw.println("import " + c.getName() + ';');
        }
        pw.println("import " + info.getTest().getQualifiedName() + ";");
        if (info.getResult() != null) {
            pw.println("import " + info.getResult().getQualifiedName() + "_jcstress;");
        }
        if (!info.getTest().equals(info.getState())) {
            if (info.getState() != null) {
                pw.println("import " + getPackageName(info.getState()) + "." + getGeneratedName(info.getState()) + ";");
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
