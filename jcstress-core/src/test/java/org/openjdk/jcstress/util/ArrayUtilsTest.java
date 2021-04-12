package org.openjdk.jcstress.util;

import org.junit.Assert;
import org.junit.Test;

public class ArrayUtilsTest {

    @Test
    public void testConcat() {
        Assert.assertArrayEquals("add one element to array of two",
                new String[]{"1", "2", "3"},
                ArrayUtils.concat(new String[]{"1", "2"}, "3"));

        Assert.assertArrayEquals("add one element to empty array",
                new String[]{"1"},
                ArrayUtils.concat(new String[0], "1"));
    }

}
