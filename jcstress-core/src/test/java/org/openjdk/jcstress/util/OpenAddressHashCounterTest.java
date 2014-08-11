package org.openjdk.jcstress.util;

import junit.framework.Assert;
import org.junit.Test;

public class OpenAddressHashCounterTest {

    @Test
    public void test1() {
        Counter<String> cnt = new OpenAddressHashCounter<>();
        cnt.record("Foo");

        Assert.assertEquals(1, cnt.count("Foo"));
        Assert.assertEquals(1, cnt.elementSet().size());
        Assert.assertEquals("Foo", cnt.elementSet().iterator().next());
    }

    @Test
    public void test2() {
        Counter<String> cnt = new OpenAddressHashCounter<>();
        cnt.record("Foo", 2);

        Assert.assertEquals(2, cnt.count("Foo"));
        Assert.assertEquals(1, cnt.elementSet().size());
        Assert.assertEquals("Foo", cnt.elementSet().iterator().next());
    }

    @Test
    public void test3() {
        Counter<String> cnt = new OpenAddressHashCounter<>();
        cnt.record("Foo", 1);
        cnt.record("Bar", 1);

        Assert.assertEquals(1, cnt.count("Foo"));
        Assert.assertEquals(1, cnt.count("Bar"));
        Assert.assertEquals(2, cnt.elementSet().size());
    }

    @Test
    public void test4() {
        Counter<String> cnt = new OpenAddressHashCounter<>();
        for (int c = 0; c < 1000; c++) {
            cnt.record("Foo" + c, c);
        }

        for (int c = 0; c < 1000; c++) {
            Assert.assertEquals(c, cnt.count("Foo" + c));
        }

        Assert.assertEquals(1000, cnt.elementSet().size());
    }

}
