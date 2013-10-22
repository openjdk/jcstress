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
package org.openjdk.jcstress.util;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class InterfaceListProcessor extends AbstractProcessor {

    private final List<String> lines = new ArrayList<String>();

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Override.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<TypeElement> visited = new HashSet<TypeElement>();
        try {
            if (!roundEnv.processingOver()) {
                for (Element element : roundEnv.getElementsAnnotatedWith(Override.class)) {
                    if (element.getModifiers().contains(Modifier.ABSTRACT)) continue;
                    TypeElement el = (TypeElement) element.getEnclosingElement();
                    if (visited.add(el)) {
                        lines.add(processingEnv.getElementUtils().getBinaryName(el).toString());
                        for (TypeElement intf : getAllInterfaces(el)) {
                            lines.add(processingEnv.getElementUtils().getBinaryName(intf).toString());
                        }
                        lines.add("");
                    }
                }
            } else {
                try {
                    FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", Reflections.INTERFACE_LIST.substring(1));
                    PrintWriter writer = new PrintWriter(file.openWriter());
                    for (String line : lines) {
                        writer.println(line);
                    }
                    writer.close();
                } catch (IOException ex) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error writing MicroBenchmark list " + ex);
                }

                // do nothing
            }
        } catch (Throwable t) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Annotation processor had thrown exception: " + t);
            t.printStackTrace(System.err);
        }
        return true;
    }

    private Collection<TypeElement> getAllInterfaces(TypeElement t) {
        if (t == null) return Collections.emptySet();

        Collection<TypeElement> result = new HashSet<TypeElement>();
        for (TypeMirror intf : t.getInterfaces()) {
            TypeElement intft = (TypeElement) processingEnv.getTypeUtils().asElement(intf);
            result.add(intft);
            result.addAll(getAllInterfaces(intft));
        }

        TypeMirror superK = t.getSuperclass();
        if (superK != null) {
            result.addAll(getAllInterfaces((TypeElement) processingEnv.getTypeUtils().asElement(superK)));
        }
        return result;
    }
}
