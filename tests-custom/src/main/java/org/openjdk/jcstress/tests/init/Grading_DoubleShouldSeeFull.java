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
package org.openjdk.jcstress.tests.init;

import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.Ref;

@Outcome(id = "[NaN]",  expect = Expect.ACCEPTABLE, desc = "The value set by the actor thread. Observer sees the complete update.")
@Outcome(id = "[42.0]", expect = Expect.ACCEPTABLE, desc = "The observer sees the empty shell. This is a legal JMM behavior, since there is a race between actor and observer.")
@Outcome(               expect = Expect.ACCEPTABLE_SPEC, desc = "Seeing the torn value. This is specifically allowed by JLS. This is not a surprising behavior on some 32-bit systems which do not have full-width 64-bit instructions.")
@Ref("http://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.7")
public class Grading_DoubleShouldSeeFull {
}
