/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jcstress.infra;

import org.openjdk.jcstress.annotations.Expect;

import java.util.ArrayList;
import java.util.Collection;

public class TestInfo {
    private final String name;
    private final String description;
    private final String runner;
    private final int threads;
    private final boolean requiresFork;
    private final Collection<StateCase> stateCases;
    private StateCase unmatched;
    private final Collection<String> refs;

    public TestInfo(String name, String runner, String description, int threads, boolean requiresFork) {
        this.name = name;
        this.runner = runner;
        this.description = description;
        this.threads = threads;
        this.requiresFork = requiresFork;
        this.stateCases = new ArrayList<StateCase>();
        this.refs = new ArrayList<String>();
    }

    public StateCase unmatched() {
        if (unmatched != null) {
            return unmatched;
        } else {
            return new StateCase("", Expect.FORBIDDEN, "No default case provided, assume " + Expect.FORBIDDEN);
        }
    }

    public Collection<StateCase> cases() {
        return stateCases;
    }

    public void addCase(StateCase aStateCase) {
        if (aStateCase.state().trim().isEmpty()) {
            unmatched = aStateCase;
        } else {
            stateCases.add(aStateCase);
        }
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String generatedRunner() {
        return runner;
    }

    public int threads() {
        return threads;
    }

    public boolean requiresFork() {
        return requiresFork;
    }

    public void addRef(String ref) {
        refs.add(ref);
    }

    public Collection<String> refs() {
        return refs;
    }
}
