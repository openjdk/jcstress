package org.openjdk.jcstress.util;

import org.junit.Assert;
import org.junit.Test;

public class TestLineTest {

    @Test
    public void test() {
        TestLineWriter writer = new TestLineWriter();
        writer.put("jcstress");
        writer.put("is cool in the year");
        writer.put(2016);
        writer.put(" = ");
        writer.put(true);
        writer.put("");
        writer.put("Yeah.");

        String s = writer.get();

        TestLineReader reader = new TestLineReader(s);

        Assert.assertEquals("jcstress", reader.nextString());
        Assert.assertEquals("is cool in the year", reader.nextString());
        Assert.assertEquals(2016, reader.nextInt());
        Assert.assertEquals(" = ", reader.nextString());
        Assert.assertEquals(true, reader.nextBoolean());
        Assert.assertEquals("", reader.nextString());
        Assert.assertEquals("Yeah.", reader.nextString());
    }

}
