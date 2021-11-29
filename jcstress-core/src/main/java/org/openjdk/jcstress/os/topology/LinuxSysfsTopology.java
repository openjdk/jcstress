/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jcstress.os.topology;

import org.openjdk.jcstress.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LinuxSysfsTopology extends AbstractTopology {

    private final Path root;

    private int readInt(Path path) throws IOException, TopologyParseException {
        List<String> lines = Files.readAllLines(path);
        if (lines.size() > 0) {
            return Integer.parseInt(lines.get(0));
        } else {
            throw new TopologyParseException("Cannot read " + path);
        }
    }

    private List<Integer> readList(Path path) throws IOException, TopologyParseException {
        List<String> lines = Files.readAllLines(path);
        if (lines.size() > 0) {
            return StringUtils.decodeCpuList(lines.get(0));
        } else {
            throw new TopologyParseException("Cannot read " + path);
        }
    }

    public LinuxSysfsTopology() throws TopologyParseException {
        this(new File("/sys/devices/system/cpu/").toPath());
    }

    public LinuxSysfsTopology(Path root) throws TopologyParseException {
        this.root = root;

        // Parse the number of available CPUs
        int cpuCount = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
            for (Path d : ds) {
                if (!Files.isDirectory(d.resolve("topology"))) continue;

                String basename = d.getFileName().toString();
                if (basename.matches("cpu[0-9]+")) {
                    cpuCount++;
                }
            }
        } catch (Exception e) {
            // Nothing to do
        }

        // Parse the package groups
        List<List<Integer>> coreGroups = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
            for (Path d : ds) {
                if (!Files.isDirectory(d.resolve("topology"))) continue;

                String basename = d.getFileName().toString();
                if (basename.matches("cpu[0-9]+")) {
                    List<Integer> list = readList(d.resolve("topology/package_cpus_list"));
                    coreGroups.add(list);
                }
            }
        } catch (Exception e) {
            // Nothing to do, fallback to package_id
            coreGroups.clear();
        }

        Collections.sort(coreGroups, (l1, l2) -> {
            for (int i = 0; i < Math.min(l1.size(), l2.size()); i++) {
                int c = Integer.compare(l1.get(i), l2.get(i));
                if (c != 0) {
                    return c;
                }
            }
            return Integer.compare(l1.size(), l2.size());
        });

        // Renumber packages here
        Map<List<Integer>, Integer> knownPackage = new HashMap<>();
        int packageCount = 0;
        for (List<Integer> cg : coreGroups) {
            if (!knownPackage.containsKey(cg)) {
                knownPackage.put(cg, packageCount++);
            }
        }

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
            boolean found = false;
            for (Path d : ds) {
                if (!Files.isDirectory(d.resolve("topology"))) continue;

                String basename = d.getFileName().toString();
                if (basename.matches("cpu[0-9]+")) {
                    int threadId = Integer.parseInt(basename.substring(3));
                    int coreId = readInt(d.resolve("topology/core_id"));
                    int packageId = readInt(d.resolve("topology/physical_package_id"));
                    if (packageId == -1) {
                        List<Integer> list = readList(d.resolve("topology/package_cpus_list"));
                        if (!knownPackage.containsKey(list)) {
                            throw new TopologyParseException("Cannot figure out package ID");
                        }
                        packageId = knownPackage.get(list);
                    }
                    add(packageId, packageId*cpuCount + coreId, threadId);
                    found = true;
                }
            }
            if (!found) {
                throw new TopologyParseException("No CPUs found");
            }
        } catch (Exception e) {
            throw new TopologyParseException(e);
        }

        renumberPackages();
        renumberCores();
        finish();
    }

    public void printStatus(PrintStream pw) {
        pw.println("  Linux, using " + root);
        super.printStatus(pw);
    }

    @Override
    public boolean trustworthy() {
        return true;
    }

}
