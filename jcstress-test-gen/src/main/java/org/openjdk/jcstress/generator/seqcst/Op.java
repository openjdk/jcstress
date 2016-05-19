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
package org.openjdk.jcstress.generator.seqcst;

import java.util.Objects;

public abstract class Op {

    private final Type type;
    protected final int varId;

    public static Op newLoad(int varId) {
        return new LoadOp(varId);
    }

    public static Op newLoad(Op op, Result res) {
        LoadOp t = new LoadOp(op.getVarId());
        t.setResult(res);
        return t;
    }

    public static Op newStore(int varId) {
        return new StoreOp(varId);
    }

    public static Op newStore(Op op, Value value) {
        StoreOp t = new StoreOp(op.getVarId());
        t.setValue(value);
        return t;
    }

    private Op(Type type, int varId) {
        this.type = type;
        this.varId = varId;
    }

    public Type getType() {
        return type;
    }

    public boolean isLoad() {
        return type == Type.LOAD;
    }

    public boolean isStore() {
        return type == Type.STORE;
    }

    public int getVarId() {
        return varId;
    }

    public abstract Result getResult();
    public abstract void setResult(Result res);

    public abstract Value getValue();
    public abstract void setValue(Value value);

    public static class LoadOp extends Op {
        private Result res;

        public LoadOp(int varId) {
            super(Type.LOAD, varId);
        }

        @Override
        public Result getResult() {
            return Objects.requireNonNull(res);
        }

        @Override
        public void setResult(Result res) {
            this.res = res;
        }

        @Override
        public Value getValue() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setValue(Value value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "r" + res + " = x" + varId;
        }

    }

    public static class StoreOp extends Op {
        private Value value;

        public StoreOp(int varId) {
            super(Type.STORE, varId);
        }

        @Override
        public Result getResult() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setResult(Result res) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Value getValue() {
            if (value == null) {
                throw new IllegalStateException("valueId unset");
            }
            return value;
        }

        @Override
        public void setValue(Value value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "x" + varId + " = C" + value;
        }

    }

    public enum Type {
        LOAD,
        STORE,
    }

}
