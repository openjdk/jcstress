package org.openjdk.jcstress.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class StringUtilsTest {

    @Test
    public void test() {
        String s = "FooBar";

        Assert.assertEquals("FooBar", StringUtils.cutoff(s, 7));
        Assert.assertEquals("FooBar", StringUtils.cutoff(s, 6));
        Assert.assertEquals("Fo...", StringUtils.cutoff(s, 5));
        Assert.assertEquals("...", StringUtils.cutoff(s, 3));
    }

    @Test
    public void testGetStacktrace() {
        String actual = StringUtils.getStacktrace(new NullPointerException("my message"));
        String firstLine = StringUtils.getFirstLine(actual);

        Assert.assertEquals("java.lang.NullPointerException: my message", firstLine);
    }

    @Test
    public void testGetFirstLine() {
        Assert.assertEquals("First line", StringUtils.getFirstLine("First line"));
        Assert.assertEquals("First line", StringUtils.getFirstLine("First line\nsecond line"));
    }

    @Test
    public void testDecodeCpuList() {
        Assert.assertEquals(Arrays.asList(0), StringUtils.decodeCpuList("0"));
        Assert.assertEquals(Arrays.asList(0, 1), StringUtils.decodeCpuList("0-1"));
        Assert.assertEquals(Arrays.asList(0, 1, 2), StringUtils.decodeCpuList("0-2"));
        Assert.assertEquals(Arrays.asList(4, 0, 1, 2), StringUtils.decodeCpuList("4,0-2"));
        Assert.assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 16, 17, 18), StringUtils.decodeCpuList("0-7,16-18"));
    }

}
