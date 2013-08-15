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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class Reflections {

    private static volatile boolean INITED;
    private static Set<String> RESOURCES;

    public static Collection<String> getResources(String filter, String postfix) {
        try {
            ensureInited();

            List<String> res = new ArrayList<String>();
            for (String name : RESOURCES) {
                if (!name.contains(filter)) continue;
                if (!name.endsWith(postfix)) continue;
                res.add(name);
            }

            return res;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Collection<Class> findAllClassesImplementing(Class<?> intf, String filter) {
        try {
            List<Class> result = new ArrayList<Class>();
            for (Class klass : getClasses(filter)) {
                if (klass.isInterface()) continue;
                if (Modifier.isAbstract(klass.getModifiers())) continue;
                if (intf.isAssignableFrom(klass) && !klass.isInterface()) {
                    result.add(klass);
                }
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Exception ", e);
        }
    }

    private static void ensureInited() throws IOException {
        if (!INITED) {
            final SortedSet<String> newResources = new TreeSet<String>();
            try {
                enumerate(new ResultCallback() {
                    @Override
                    public void accept(String name) {
                        newResources.add(name);
                    }
                });
            } catch (Throwable t) {
                throw new IOException(t);
            }
            RESOURCES = newResources;
            INITED = true;
        }
    }

    public static Collection<Class> getClasses(final String filter) throws IOException {
        final List<Class> newClasses = new ArrayList<Class>();
        for (String name : getClassNames(filter)) {
            try {
                if (name.contains("VMSupport")) continue;
                newClasses.add(Class.forName(name));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        return newClasses;
    }

    public static Collection<String> getClassNames(final String filter) throws IOException {
        ensureInited();

        final List<String> newClasses = new ArrayList<String>();
        for (String name : RESOURCES) {
            name = name.replaceAll("\\\\", ".");
            name = name.replaceAll("/", ".");
            if (name.contains(filter) && name.endsWith(".class")) {
                newClasses.add(name.substring(name.indexOf(filter), name.length() - 6));
            }
        }

        return newClasses;
    }

    public static void enumerate(ResultCallback callback) throws Throwable {
        for (String element : System.getProperty("java.class.path").split(File.pathSeparator)) {
            enumerate(callback, element);
        }
    }

    public static void enumerate(ResultCallback callback, String element) throws Throwable {
        File file = new File(element);
        if (file.getName().endsWith(".jar")) {
            enumerateJAR(file, callback);
        } else {
            enumeratePath(element, callback);
        }
    }

    private static void enumeratePath(String dir, final ResultCallback callback) throws IOException {

        List<File> dirs = new ArrayList<File>();
        dirs.add(new File(dir));

        while (!dirs.isEmpty()) {

            List<File> siblings = new ArrayList<File>();
            for (File d : dirs) {
                for (File f : d.listFiles()) {
                    if (f.isDirectory()) {
                        siblings.add(f);
                    }
                    callback.accept(f.toString());
                }
            }

            dirs = siblings;
        }
    }

    private static void enumerateJAR(File jar, ResultCallback callback) throws Throwable {
        FileInputStream jarFileStream = new FileInputStream(jar);
        JarInputStream jarFile = new JarInputStream(jarFileStream);

        JarEntry jarEntry;
        while ((jarEntry = jarFile.getNextJarEntry()) != null) {
            callback.accept(jarEntry.getName());
        }

        jarFileStream.close();
        jarFile.close();

    }

    public static void copy(InputStream input, OutputStream output) throws IOException {
        if (input == null) {
            throw new IOException("inputstream is null");
        }

        byte[] buffer = new byte[8192];
        while (true) {
            int read = input.read(buffer);
            if (read == -1) {
                break;
            }
            output.write(buffer, 0, read);
        }
    }

    public static byte[] readAll(File file) throws IOException {
        FileInputStream fos = new FileInputStream(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(fos, baos);
        fos.close();
        return baos.toByteArray();
    }

    public static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(input, baos);
        return baos.toByteArray();
    }


    public interface ResultCallback {
        void accept(String name);
    }

}
