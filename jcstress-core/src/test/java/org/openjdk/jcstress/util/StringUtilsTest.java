package org.openjdk.jcstress.util;

import junit.framework.Assert;
import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void test() {
        String s = "FooBar";

        Assert.assertEquals("FooBar", StringUtils.cutoff(s, 7));
        Assert.assertEquals("FooBar", StringUtils.cutoff(s, 6));
        Assert.assertEquals("Fo...", StringUtils.cutoff(s, 5));
        Assert.assertEquals("...", StringUtils.cutoff(s, 3));
    }

}
