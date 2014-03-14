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
package org.openjdk.jcstress.infra.processors;

import org.openjdk.jcstress.Options;
import org.openjdk.jcstress.infra.annotations.Actor;
import org.openjdk.jcstress.infra.annotations.Arbiter;
import org.openjdk.jcstress.infra.annotations.ConcurrencyStressTest;
import org.openjdk.jcstress.infra.annotations.Result;
import org.openjdk.jcstress.infra.annotations.State;
import org.openjdk.jcstress.infra.collectors.TestResultCollector;
import org.openjdk.jcstress.infra.runners.Control;
import org.openjdk.jcstress.infra.runners.Runner;
import org.openjdk.jcstress.infra.runners.StateHolder;
import org.openjdk.jcstress.infra.runners.TestList;
import org.openjdk.jcstress.util.ArrayUtils;
import org.openjdk.jcstress.util.Counter;
import org.openjdk.jcstress.util.Counters;
import org.openjdk.jcstress.util.Reflections;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;


@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ConcurrencyStressTestProcessor extends AbstractProcessor {

    private final List<TestInfo> tests = new ArrayList<TestInfo>();

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(ConcurrencyStressTest.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            Set<? extends Element> set = roundEnv.getElementsAnnotatedWith(ConcurrencyStressTest.class);
            for (Element el : set) {
                TypeElement e = (TypeElement) el;
                try {
                    TestInfo info = parseAndValidate(e);
                    generate(info);
                    tests.add(info);
                } catch (GenerationException ex) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ex.getMessage(), ex.getElement());
                }
            }
        } else {
            try {
                FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", TestList.LIST.substring(1));
                PrintWriter writer = new PrintWriter(file.openWriter());
                for (TestInfo test : tests) {
                    writer.println(test.getTest().getQualifiedName() + "," + test.getGeneratedName());
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

        for (ExecutableElement method : ElementFilter.methodsIn(e.getEnclosedElements())) {
            if (method.getAnnotation(Actor.class) != null) {
                info.addActor(method);
            }

            if (method.getAnnotation(Arbiter.class) != null) {
                info.setArbiter(method);
            }

            for (VariableElement var : method.getParameters()) {
                TypeElement paramClass = (TypeElement) processingEnv.getTypeUtils().asElement(var.asType());
                if (paramClass.getAnnotation(State.class) != null) {
                    info.setState(paramClass);
                } else if (paramClass.getAnnotation(Result.class) != null) {
                    info.setResult(paramClass);
                } else {
                    throw new GenerationException("The parameter for @" + Actor.class.getSimpleName() +
                            " methods requires either @" + State.class.getSimpleName() + " or @" + Result.class.getSimpleName() +
                            " annotated class", var);
                }
            }
        }

        if (e.getAnnotation(State.class) != null) {
            info.setState(e);
        } else if (e.getAnnotation(Result.class) != null) {
            info.setResult(e);
        }

        if (info.getState() == null) {
            throw new GenerationException("@" + ConcurrencyStressTest.class.getSimpleName() + " defines no @" +
                    State.class.getSimpleName() + " to work with", e);
        }

        if (info.getResult() == null) {
            throw new GenerationException("@" + ConcurrencyStressTest.class.getSimpleName() + " defines no @" +
                    Result.class.getSimpleName() + " to work with", e);
        }

        String packageName = getPackageName(info.getTest()) + ".generated";
        String testName = getGeneratedName(info.getTest());

        info.setGeneratedName(packageName + "." + testName);

        return info;
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
                name = ((TypeElement)ci).getQualifiedName() + (name.isEmpty() ? "" : "$" + name);
                break;
            } else {
                name = ci.getSimpleName() + (name.isEmpty() ? "" : "$" + name);
            }
            ci = parent;
        }
        return name;
    }

    private void generate(TestInfo info) {
        PrintWriter pw;
        Writer writer;
        try {
            writer = processingEnv.getFiler().createSourceFile(getPackageName(info.getTest()) + ".generated." + getGeneratedName(info.getTest())).openWriter();
            pw = new PrintWriter(writer);
        } catch (IOException e) {
            throw new GenerationException("IOException: " + e.getMessage(), info.getTest());
        }

        String t = info.getTest().getSimpleName().toString();
        String s = info.getState().getSimpleName().toString();
        String r = info.getResult().getSimpleName().toString();

        int actorsCount = info.getActors().size();

        pw.println("package " + getPackageName(info.getTest()) + ".generated;");

        printImports(pw, info);

        pw.println("public class " + getGeneratedName(info.getTest()) + " extends Runner<" + r + "> {");
        pw.println();

        pw.println("    public " + getGeneratedName(info.getTest()) + "(Options opts, TestResultCollector collector, ExecutorService pool) {");
        pw.println("        super(opts, collector, pool, \"" + getQualifiedName(info.getTest()) + "\");");
        pw.println("    }");
        pw.println();

        pw.println("    @Override");
        pw.println("    public int requiredThreads() {");
        pw.println("        return " + actorsCount + ";");
        pw.println("    }");
        pw.println();

        pw.println("    @Override");
        pw.println("    public void sanityCheck() throws Throwable {");
        pw.println("        " + t + " t = new " + t + "();");
        pw.println("        " + s + " s = new " + s + "();");
        pw.println("        " + r + " r = new " + r + "();");

        for (ExecutableElement el : info.getActors()) {
            emitMethod(pw, el, "        t." + el.getSimpleName(), "s", "r");
        }
        if (info.getArbiter() != null) {
            emitMethod(pw, info.getArbiter(), "        t." + info.getArbiter().getSimpleName(), "s", "r");
        }

        pw.println("    }");
        pw.println();

        pw.println("    @Override");
        pw.println("    public Counter<" + r + "> internalRun() {");
        pw.println("        " + t + " test = new " + t + "();");
        pw.println("        " + s + "[] poison = new " + s + "[0];");
        pw.println();
        pw.println("        Counter<" + r + "> counter = Counters.newCounter(" + r + ".class);");
        pw.println();
        pw.println("        " + s + "[] newStride = new " + s + "[control.minStride];");
        pw.println("        for (int c = 0; c < control.minStride; c++) {");
        pw.println("            newStride[c] = new " + s + "();");
        pw.println("        }");
        pw.println();
        pw.println("        " + r + "[] newResult = new " + r + "[control.minStride];");
        pw.println("        for (int c = 0; c < control.minStride; c++) {");
        pw.println("            newResult[c] = new " + r + "();");
        pw.println("        }");
        pw.println();
        pw.println("        int[] indices = ArrayUtils.generatePermutation(control.minStride);");
        pw.println("        StateHolder<" + s + ", " + r + "> holder = new StateHolder<" + s + ", " + r + ">(newStride, newResult, indices, " + actorsCount + ");");
        pw.println();
        pw.println("        final AtomicReference<StateHolder<" + s + "," + r + ">> version = new AtomicReference<StateHolder<" + s + ", " + r + ">>();");
        pw.println("        version.set(holder);");
        pw.println();
        pw.println("        final AtomicInteger epoch = new AtomicInteger();");
        pw.println();
        pw.println("        Collection<Future<?>> tasks = new ArrayList<Future<?>>();");
        pw.println();
        pw.println("        control.isStopped = false;");

        for (ExecutableElement a : info.getActors()) {
            pw.println("        tasks.add(pool.submit(new Runner_" + a.getSimpleName() + "(control, counter, test, poison, version, epoch)));");
        }

        pw.println();
        pw.println("        try {");
        pw.println("            TimeUnit.MILLISECONDS.sleep(control.time);");
        pw.println("        } catch (InterruptedException e) {");
        pw.println("        }");
        pw.println();
        pw.println("        control.isStopped = true;");
        pw.println();
        pw.println("        waitFor(tasks);");
        pw.println();
        pw.println("        return counter;");
        pw.println("    }");

        pw.println("public abstract static class RunnerBase {");
        pw.println("    final Control control;");
        pw.println("    final Counter<" + r + "> counter;");
        pw.println("    final " + t + " test;");
        pw.println("    final " + s + "[] poison;");
        pw.println("    final AtomicReference<StateHolder<" + s + "," + r + ">> version;");
        pw.println("    final AtomicInteger epoch;");
        pw.println();
        pw.println("    public RunnerBase(Control control, Counter<" + r + "> counter, " + t + " test, " + s + "[] poison, AtomicReference<StateHolder<" + s + "," + r + ">> version, AtomicInteger epoch) {");
        pw.println("        this.control = control;");
        pw.println("        this.counter = counter;");
        pw.println("        this.test = test;");
        pw.println("        this.poison = poison;");
        pw.println("        this.version = version;");
        pw.println("        this.epoch = epoch;");
        pw.println("    }");
        pw.println();
        pw.println("    public void newEpoch(StateHolder<" + s + "," + r + "> holder) {");

        pw.println("            int loops = holder.loops;");
        pw.println("            " + s + "[] cur = holder.s;");
        pw.println("            " + r + "[] res = holder.r;");

        if (info.getArbiter() != null) {
            pw.println();
            pw.println("                for (int l = 0; l < loops; l++) {");
            if (info.getState().equals(info.getTest())) {
                emitMethod(pw, info.getArbiter(), "                cur[l]." + info.getArbiter().getSimpleName(), "cur[l]", "res[l]");
            } else {
                emitMethod(pw, info.getArbiter(), "                test." + info.getArbiter().getSimpleName(), "cur[l]", "res[l]");
            }
            pw.println("                }");
        }
        pw.println();
        pw.println("                for (" + r + " r1 : res) {");
        pw.println("                    counter.record(r1);");
        pw.println("                }");
        pw.println();
        pw.println("                StateHolder<" + s + ", " + r + "> newHolder;");
        pw.println("                if (control.isStopped) {");
        pw.println("                    newHolder = new StateHolder<" + s + ", " + r + ">(poison, null, null, " + actorsCount + ");");
        pw.println("                } else {");
        pw.println("                    int newLoops = holder.hasLaggedWorkers ? Math.min(loops * 2, control.maxStride) : loops;");
        pw.println();
        pw.println("                    for (int c = 0; c < loops; c++) {");

        for (VariableElement var : ElementFilter.fieldsIn(info.getResult().getEnclosedElements())) {
            pw.print("                        res[c]." + var.getSimpleName().toString() + " = ");
            String type = var.asType().toString();
            if (type.equals("int") || type.equals("long") || type.equals("short") || type.equals("byte") || type.equals("char")) {
                pw.print("0");
            } else if (type.equals("double")) {
                pw.print("0D");
            } else if (type.equals("float")) {
                pw.print("0F");
            } else if (type.equals("boolean")) {
                pw.print("false");
            } else {
                throw new GenerationException("Unable to handle @" + Result.class.getSimpleName() + " field of type " + type, var);
            }
            pw.println(";");
        }

        pw.println("                    }");
        pw.println();
        pw.println("                    " + s + "[] newStride = cur;");
        pw.println("                    " + r + "[] newRes = res;");
        pw.println("                    int[] indices = holder.indices;");
        pw.println("                    if (newLoops > loops) {");
        pw.println("                        newStride = Arrays.copyOf(cur, newLoops);");
        pw.println("                        newRes = Arrays.copyOf(res, newLoops);");
        pw.println("                        for (int c = loops; c < newLoops; c++) {");
        pw.println("                            newRes[c] = new " + r + "();");
        pw.println("                        }");
        pw.println("                        indices = ArrayUtils.generatePermutation(newLoops);");
        pw.println("                    }");
        pw.println();
        pw.println("                    for (int c = 0; c < newLoops; c++) {");
        pw.println("                        newStride[c] = new " + s + "();");
        pw.println("                    }");
        pw.println();
        pw.println("                    newHolder = new StateHolder<" + s + ", " + r + ">(newStride, newRes, indices, " + actorsCount + ");");
        pw.println("                }");
        pw.println();
        pw.println("                version.set(newHolder);");
        pw.println("    }");
        pw.println("}");
        pw.println();

        for (ExecutableElement a : info.getActors()) {
            pw.println("public static class Runner_" + a.getSimpleName() + " extends RunnerBase implements Callable {");
            pw.println("    public Runner_" + a.getSimpleName() + "(Control control, Counter<" + r + "> counter, " + t + " test, " + s + "[] poison, AtomicReference<StateHolder<" + s + "," + r + ">> version, AtomicInteger epoch) {");
            pw.println("        super(control, counter, test, poison, version, epoch);");
            pw.println("    }");
            pw.println();
            pw.println("    public Void call() {");
            pw.println("        int curEpoch = 0;");
            pw.println();
            pw.println("        " + t + " lt = test;");
            pw.println();
            pw.println("        while (true) {");
            pw.println("            StateHolder<" + s + ", " + r + "> holder = version.get();");
            pw.println("            int loops = holder.loops;");
            pw.println("            int[] indices = holder.indices;");
            pw.println("            " + s + "[] cur = holder.s;");
            pw.println("            " + r + "[] res = holder.r;");
            pw.println();
            pw.println("            if (cur == poison) {");
            pw.println("                return null;");
            pw.println("            }");
            pw.println();
            pw.println("            holder.preRun(control.shouldYield);");
            pw.println();
            pw.println("            for (int l = 0; l < loops; l++) {");
            pw.println("                int index = indices[l];");

            if (info.getState().equals(info.getTest())) {
                emitMethod(pw, a, "                cur[index]." + a.getSimpleName(), "cur[index]", "res[index]");
            } else {
                emitMethod(pw, a, "                lt." + a.getSimpleName(), "cur[index]", "res[index]");
            }

            pw.println("            }");
            pw.println();
            pw.println("            holder.postRun(control.shouldYield);");
            pw.println();
            pw.println("            if (epoch.compareAndSet(curEpoch, curEpoch + 1)) {");
            pw.println("                newEpoch(holder);");
            pw.println("            }");
            pw.println();
            pw.println("            curEpoch++;");
            pw.println("            while (curEpoch != epoch.get()) {");
            pw.println("                if (control.shouldYield) Thread.yield();");
            pw.println("            }");
            pw.println();
            pw.println("            holder.postConsume(control.shouldYield);");
            pw.println("        }");
            pw.println("    }");
            pw.println("}");
        }
        pw.println("}");

        pw.close();
    }

    private void emitMethod(PrintWriter pw, ExecutableElement el, String lvalue, String stateAccessor, String resultAccessor) {
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
        pw.println(");");
    }

    private void printImports(PrintWriter pw, TestInfo info) {
        Class<?>[] imports = new Class<?>[] {
                Options.class, Result.class, TestResultCollector.class,
                Counter.class, Counters.class, Runner.class, Override.class, StateHolder.class,
                ArrayList.class, Collection.class, ExecutorService.class,
                Future.class, TimeUnit.class, AtomicInteger.class, AtomicReference.class, Callable.class,
                ArrayUtils.class, Arrays.class, Control.class
        };

        for (Class<?> c : imports) {
            pw.println("import " + c.getName() + ';');
        }
        pw.println("import " + info.getTest().getQualifiedName() + ";");
        pw.println("import " + info.getState().getQualifiedName() + ";");
        pw.println("import " + info.getResult().getQualifiedName() + ";");
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
