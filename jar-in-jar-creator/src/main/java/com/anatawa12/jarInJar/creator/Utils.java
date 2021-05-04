package com.anatawa12.jarInJar.creator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class Utils {
    private Utils() {
    }

    public static void copyStream(InputStream inputStream, OutputStream out, boolean closeInput) throws IOException {
        try {
            byte[] buf = new byte[4096];
            int read;
            while ((read = inputStream.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        } finally {
            if (closeInput) inputStream.close();
        }
    }

    public static byte[] readStreamToByteArray(ZipInputStream inputStream) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] data = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                outputStream.write(data, 0, bytesRead);
            }
            outputStream.flush();
            return outputStream.toByteArray();
        }
    }

    public static String makeSlashed(String dotted) {
        String slashed = dotted.replace('.', '/');
        if (slashed.contains("//")) throw new IllegalStateException("package name has .. part");
        if (!slashed.endsWith("/")) slashed = slashed + '/';
        return slashed;
    }

    public static byte[] createHash(InputStream inputStream) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
        try {
            byte[] buf = new byte[4096];
            int read;
            while ((read = inputStream.read(buf)) != -1) {
                digest.update(buf, 0, read);
            }
        } finally {
            inputStream.close();
        }
        byte[] hash = digest.digest();
        byte[] hashChars = new byte[hash.length * 2 + 1];
        for (int i = 0; i < hash.length; i++) {
            hashChars[i * 2] = (byte) "0123456789abcdef".charAt((hash[i] >> 4) & 0xF);
            hashChars[i * 2 + 1] = (byte) "0123456789abcdef".charAt(hash[i] & 0xF);
        }
        hashChars[hashChars.length - 1] = '\n';
        return hashChars;
    }
}
