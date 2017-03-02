package org.openjdk.jcstress.util;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FileUtilsTest {

    @Test
    public void testCopyFile() throws IOException {
        String tempFile = FileUtils.copyFileToTemp("/org/openjdk/jcstress/util/FileUtils.class", "fileutils", ".class");

        assertNotNull("File name returned", tempFile);
        assertTrue("File exists on disk", new File(tempFile).exists());
    }

    @Test(expected = IOException.class)
    public void testCopyFileThrowsNullPointerOnError() throws IOException {
        FileUtils.copyFileToTemp("/org/openjdk/jcstress/util/FileUtils.cl", "fileutils", ".class");
    }

}