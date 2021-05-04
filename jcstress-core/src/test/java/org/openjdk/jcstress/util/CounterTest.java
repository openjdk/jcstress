package org.openjdk.jcstress.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.*;

public class CounterTest {

    @Test
    public void test1() {
        Counter<String> cnt = new Counter<>();
        cnt.record("Foo");

        Assert.assertEquals(1, cnt.count("Foo"));
        Assert.assertEquals(1, cnt.elementSet().size());
        Assert.assertEquals("Foo", cnt.elementSet().iterator().next());
    }

    @Test
    public void test2() {
        Counter<String> cnt = new Counter<>();
        cnt.record("Foo", 2);

        Assert.assertEquals(2, cnt.count("Foo"));
        Assert.assertEquals(1, cnt.elementSet().size());
        Assert.assertEquals("Foo", cnt.elementSet().iterator().next());
    }

    @Test
    public void test3() {
        Counter<String> cnt = new Counter<>();
        cnt.record("Foo", 1);
        cnt.record("Bar", 1);

        Assert.assertEquals(1, cnt.count("Foo"));
        Assert.assertEquals(1, cnt.count("Bar"));
        Assert.assertEquals(2, cnt.elementSet().size());
    }

    @Test
    public void test4() {
        Counter<String> cnt = new Counter<>();
        for (int c = 0; c < 1000; c++) {
            cnt.record("Foo" + c, c);
        }

        for (int c = 0; c < 1000; c++) {
            Assert.assertEquals(c, cnt.count("Foo" + c));
        }

        Assert.assertEquals(1000, cnt.elementSet().size());
    }

    @Test
    public void testSerial_1() throws IOException, ClassNotFoundException {
        Counter<String> cnt = new Counter<>();
        for (int c = 0; c < 1000; c++) {
            cnt.record("Foo" + c, c);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(cnt);
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray());
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            @SuppressWarnings("unchecked")
            Counter<String> desCnt = (Counter<String>) ois.readObject();

            for (int c = 0; c < 1000; c++) {
                Assert.assertEquals(c, desCnt.count("Foo" + c));
            }

            Assert.assertEquals(1000, desCnt.elementSet().size());
        }
    }

}
