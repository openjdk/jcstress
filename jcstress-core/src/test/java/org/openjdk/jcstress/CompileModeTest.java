package org.openjdk.jcstress;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jcstress.vm.CompileMode;

public class CompileModeTest {

    @Test
    public void unified() {
        for (int a = 0; a < 2; a++) {
            Assert.assertTrue(!CompileMode.isInt(CompileMode.UNIFIED, a));
            Assert.assertTrue(!CompileMode.isC1(CompileMode.UNIFIED, a));
            Assert.assertTrue(!CompileMode.isC2(CompileMode.UNIFIED, a));
        }

        for (int a = 0; a < 4; a++) {
            Assert.assertTrue(CompileMode.hasC2(CompileMode.UNIFIED, a));
        }
    }

    @Test
    public void splitComplete_1() {
        int[] cases = CompileMode.casesFor(1, true, true);

        // Check all these configs are present:
        for (int a0 = 0; a0 < CompileMode.MAX_MODES; a0++) {
            boolean ex = false;
            for (int cm : cases) {
                ex |= select(a0, cm, 0);
            }
            Assert.assertTrue("Mode does not exist: " + a0, ex);
        }
    }

    @Test
    public void splitComplete_2() {
        int[] cases = CompileMode.casesFor(2, true, true);

        // Check all these configs are present:
        for (int a0 = 0; a0 < CompileMode.MAX_MODES; a0++) {
            for (int a1 = 0; a1 < CompileMode.MAX_MODES; a1++) {
                boolean ex = false;
                for (int cm : cases) {
                    ex |= select(a0, cm, 0) &&
                          select(a1, cm, 1);
                }
                Assert.assertTrue("Mode does not exist: " + a0 + ", " + a1, ex);
            }
        }
    }

    @Test
    public void splitComplete_3() {
        int[] cases = CompileMode.casesFor(3, true, true);

        // Check all these configs are present:
        for (int a0 = 0; a0 < CompileMode.MAX_MODES; a0++) {
            for (int a1 = 0; a1 < CompileMode.MAX_MODES; a1++) {
                for (int a2 = 0; a2 < CompileMode.MAX_MODES; a2++) {
                    boolean ex = false;
                    for (int cm : cases) {
                        ex |= select(a0, cm, 0) &&
                              select(a1, cm, 1) &&
                              select(a2, cm, 2);
                    }
                    Assert.assertTrue("Mode does not exist: " + a0 + ", " + a1 + ", " + a2, ex);
                }
            }
        }
    }

    @Test
    public void splitOnlyC1_3() {
        boolean hasInt = false;
        boolean hasC1 = false;
        boolean hasC2 = false;

        for (int cm : CompileMode.casesFor(3, true, false)) {
            for (int a = 0; a < 3; a++) {
                hasInt |= CompileMode.isInt(cm, a);
                hasC1 |= CompileMode.isC1(cm, a);
                hasC2 |= CompileMode.isC2(cm, a);
            }
            Assert.assertFalse("C2 modes should not exist: " + cm, hasC2);
        }

        Assert.assertTrue("Interpreter modes should not exist", hasInt);
        Assert.assertTrue("C1 modes should exist", hasC1);
    }

    @Test
    public void splitOnlyC2_3() {
        boolean hasInt = false;
        boolean hasC1 = false;
        boolean hasC2 = false;

        for (int cm : CompileMode.casesFor(3, false, true)) {
            for (int a = 0; a < 3; a++) {
                hasInt |= CompileMode.isInt(cm, a);
                hasC1 |= CompileMode.isC1(cm, a);
                hasC2 |= CompileMode.isC2(cm, a);
            }
            Assert.assertFalse("C1 modes should not exist: " + cm, hasC1);
        }

        Assert.assertTrue("Interpreter modes should exist", hasInt);
        Assert.assertTrue("C2 modes should exist", hasC2);
    }

    @Test
    public void splitOnlyInt_3() {
        boolean hasInt = false;
        boolean hasC1 = false;
        boolean hasC2 = false;

        for (int cm : CompileMode.casesFor(3, false, false)) {
            for (int a = 0; a < 3; a++) {
                hasInt |= CompileMode.isInt(cm, a);
                hasC1 |= CompileMode.isC1(cm, a);
                hasC2 |= CompileMode.isC2(cm,a);
            }
            Assert.assertFalse("C1 modes should not exist: " + cm, hasC1);
            Assert.assertFalse("C2 modes should not exist: " + cm, hasC2);
        }

        Assert.assertTrue("Interpreter modes should exist", hasInt);
    }

    private boolean select(int m, int cm, int a) {
        switch (m) {
            case 0:
                return CompileMode.isInt(cm, a);
            case 1:
                return CompileMode.isC1(cm, a);
            case 2:
                return CompileMode.isC2(cm, a);
            default:
                throw new IllegalStateException("Unknown mode");
        }
    }
}
