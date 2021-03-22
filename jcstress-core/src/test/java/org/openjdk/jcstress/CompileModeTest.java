package org.openjdk.jcstress;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jcstress.vm.CompileMode;

import java.util.Arrays;

public class CompileModeTest {

    @Test
    public void unified() {
        CompileMode cm = new CompileMode(CompileMode.UNIFIED, Arrays.asList("actor1", "actor2"), 2);

        for (int a = 0; a < 2; a++) {
            Assert.assertTrue(!cm.isInt(a));
            Assert.assertTrue(!cm.isC1(a));
            Assert.assertTrue(!cm.isC2(a));
        }

        Assert.assertTrue(cm.hasC2());
    }

    @Test
    public void splitComplete_1() {
        int cases = CompileMode.casesFor(1);

        CompileMode[] modes = new CompileMode[cases];

        for (int c = 0; c < cases; c++) {
            modes[c] = new CompileMode(c, Arrays.asList("actor1"), 1);
        }

        // Check all these configs are present:
        for (int a0 = 0; a0 < CompileMode.VARIANTS; a0++) {
            boolean ex = false;
            for (CompileMode cm : modes) {
                ex |= select(a0, cm, 0);
            }
            Assert.assertTrue("Mode does not exist: " + a0, ex);
        }
    }

    @Test
    public void splitComplete_2() {
        int cases = CompileMode.casesFor(2);

        CompileMode[] modes = new CompileMode[cases];

        for (int c = 0; c < cases; c++) {
            modes[c] = new CompileMode(c, Arrays.asList("actor1", "actor2"), 2);
        }

        // Check all these configs are present:
        for (int a0 = 0; a0 < CompileMode.VARIANTS; a0++) {
            for (int a1 = 0; a1 < CompileMode.VARIANTS; a1++) {
                boolean ex = false;
                for (CompileMode cm : modes) {
                    ex |= select(a0, cm, 0) &&
                          select(a1, cm, 1);
                }
                Assert.assertTrue("Mode does not exist: " + a0 + ", " + a1, ex);
            }
        }
    }

    @Test
    public void splitComplete_3() {
        int cases = CompileMode.casesFor(3);

        CompileMode[] modes = new CompileMode[cases];

        for (int c = 0; c < cases; c++) {
            modes[c] = new CompileMode(c, Arrays.asList("actor1", "actor2", "actor3"), 3);
        }

        // Check all these configs are present:
        for (int a0 = 0; a0 < CompileMode.VARIANTS; a0++) {
            for (int a1 = 0; a1 < CompileMode.VARIANTS; a1++) {
                for (int a2 = 0; a2 < CompileMode.VARIANTS; a2++) {
                    boolean ex = false;
                    for (CompileMode cm : modes) {
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
    public void splitComplete_4() {
        int cases = CompileMode.casesFor(4);

        CompileMode[] modes = new CompileMode[cases];

        for (int c = 0; c < cases; c++) {
            modes[c] = new CompileMode(c, Arrays.asList("actor1", "actor2", "actor3", "actor4"), 4);
        }

        // Check all these configs are present:
        for (int a0 = 0; a0 < CompileMode.VARIANTS; a0++) {
            for (int a1 = 0; a1 < CompileMode.VARIANTS; a1++) {
                for (int a2 = 0; a2 < CompileMode.VARIANTS; a2++) {
                    for (int a3 = 0; a3 < CompileMode.VARIANTS; a3++) {
                        boolean ex = false;
                        for (CompileMode cm : modes) {
                            ex |= select(a0, cm, 0) &&
                                  select(a1, cm, 1) &&
                                  select(a2, cm, 2) &&
                                  select(a3, cm, 3);
                        }
                        Assert.assertTrue("Mode does not exist: " + a0 + ", " + a1 + ", " + a2 + ", " + a3, ex);
                    }
                }
            }
        }
    }

    private boolean select(int m, CompileMode cm, int a) {
        switch (m) {
            case 0:
                return cm.isInt(a);
            case 1:
                return cm.isC1(a);
            case 2:
                return cm.isC2(a);
            default:
                throw new IllegalStateException("Unknown mode");
        }
    }
}
