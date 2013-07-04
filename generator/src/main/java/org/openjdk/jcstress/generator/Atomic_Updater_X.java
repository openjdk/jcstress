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

public class Atomic_Updater_X implements Primitive {

    private String unitValue;
    private String defaultValue;
    private String rValue;
    private String setValue;

    private final Class<?> guardType;
    private final Class<?> primType;
    private final AcqType acqType;
    private final RelType relType;

    public Atomic_Updater_X(Class<?> guardType, Class<?> primType, AcqType acqType, RelType relType) {
        this.guardType = guardType;
        this.primType = primType;
        this.acqType = acqType;
        this.relType = relType;

        unitValue = TestGenerator.getUnitValue(primType);
        defaultValue = TestGenerator.getDefaultValue(primType);
        rValue = TestGenerator.getRValue(primType);
        setValue = TestGenerator.getSetValue(primType);
    }

    @Override
    public String printStateField() {
        return "final " + guardType.getSimpleName() + "<State> g = " + guardType.getSimpleName() + ".<State>newUpdater(State.class, \"v\");" + "\n"
               + "volatile " + primType.getSimpleName() + " v;";
    }

    @Override
    public String printAcquire(String region) {
        switch (acqType) {
            case CAS:
                return String.format("r.r1 = s.g.compareAndSet(s, %s, %s) ? %s : %s; \n" + region,
                        setValue,
                        defaultValue,
                        setValue,
                        defaultValue
                );
            case get:
                return "r.r1 = s.g.get(s) == " + defaultValue + "? " + defaultValue + " : " + setValue + " ; \n" + region;
            case incrementAndGet:
                return "r.r1 = s.g.incrementAndGet(s) == (" + defaultValue + " + " + unitValue + ") ? " + defaultValue + " : " + setValue + "; \n" + region;
            case getAndIncrement:
                return "r.r1 = s.g.getAndIncrement(s) == " + defaultValue + "? " + defaultValue + " : " + setValue + " ; \n" + region;
            case decrementAndGet:
                return "r.r1 = s.g.decrementAndGet(s) == (" + defaultValue + " - " + unitValue + ") ? " + defaultValue + " : " + setValue + "; \n" + region;
            case getAndDecrement:
                return "r.r1 = s.g.getAndDecrement(s) == " + defaultValue + "? " + defaultValue + " : " + setValue + " ; \n" + region;
            case addAndGet:
                return "r.r1 = s.g.addAndGet(s, " + rValue + ") == (" + defaultValue + " + " + rValue + ") ? " + defaultValue + " : " + setValue + "; \n" + region;
            case getAndAdd:
                return "r.r1 = s.g.getAndAdd(s, " + rValue + ") == " + defaultValue + "? " + defaultValue + " : " + setValue + " ; \n" + region;
            case getAndSet:
                return "r.r1 = s.g.getAndSet(s, " + rValue + ") == " + defaultValue + "? " + defaultValue + " : " + setValue + " ; \n" + region;
            default:
                throw new IllegalStateException("" + acqType);
        }
    }

    @Override
    public String printRelease(String region) {
        switch (relType) {
            case set:
                return region + "s.g.set(s, " +setValue + ");";
            case CAS:
                return region + "s.g.compareAndSet(s, " + defaultValue + ", " +setValue + ");";
            case incrementAndGet:
                return region + "s.g.incrementAndGet(s);";
            case getAndIncrement:
                return region + "s.g.getAndIncrement(s);";
            case decrementAndGet:
                return region + "s.g.decrementAndGet(s);";
            case getAndDecrement:
                return region + "s.g.getAndDecrement(s);";
            case addAndGet:
                return region + "s.g.addAndGet(s, " + rValue + ");";
            case getAndAdd:
                return region + "s.g.getAndAdd(s, " + rValue + ");";
            case getAndSet:
                return region + "s.g.getAndSet(s, " + rValue + ");";
            default:
                throw new IllegalStateException("" + relType);
        }
    }

}
