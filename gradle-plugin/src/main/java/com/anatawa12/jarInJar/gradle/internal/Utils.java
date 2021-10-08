package com.anatawa12.jarInJar.gradle.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.zip.ZipOutputStream;

public class Utils {
    public static void copyStream(InputStream inputStream, OutputStream out) throws IOException {
        try {
            byte[] buf = new byte[4096];
            int read;
            while ((read = inputStream.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        } finally {
            inputStream.close();
        }
    }

    public static <T> Callable<T> callable(Callable<T> callable) {
        return callable;
    }
}
