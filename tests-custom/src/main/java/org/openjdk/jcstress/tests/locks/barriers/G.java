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
package org.openjdk.jcstress.tests.locks.barriers;

import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.Outcome;

@Description("Tests if the synchronized section provides the essential barriers")
@Outcome(id = {"[0, 0]", "[0, 2]"}, expect = Expect.ACCEPTABLE, desc = "The write to $b is not visible yet; we can observe whatever in $a.")
@Outcome(id = {"[1, 2]"},           expect = Expect.ACCEPTABLE, desc = "The write to $b is observed, expected to see $a == 1.")
@Outcome(id = {"[1, 0]"},           expect = Expect.ACCEPTABLE_INTERESTING,
        desc = "The write to $b is observed, but write to $a is not. This is the counter-intuitive behavior," +
               "but the coding pattern is incorrect: alone \"sync\" barrier is not enough to get the proper" +
               "acquire/release semantics.")
public class G {
}
