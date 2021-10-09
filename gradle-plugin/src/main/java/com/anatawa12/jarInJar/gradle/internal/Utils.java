package com.anatawa12.jarInJar.gradle.internal;

import org.gradle.api.UncheckedIOException;
import org.gradle.process.JavaExecSpec;
import org.gradle.util.GradleVersion;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

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

    private static final BiConsumer<JavaExecSpec, String> setMain;

    static {
        if (GradleVersion.current().compareTo(GradleVersion.version("7.0")) >= 0) {
            // 7.0/later: use mainClass property
            setMain = (spec, main) -> spec.getMainClass().set(main);
        } else {
            final Method setMainFn;
            try {
                setMainFn = JavaExecSpec.class.getMethod("setMain", String.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            setMain = (spec, main) -> {
                try {
                    setMainFn.invoke(spec, main);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw sneakyThrow(e);
                }
            };
        }
    }

    public static void setMain(JavaExecSpec spec, String main) {
        setMain.accept(spec, main);
    }

    public static RuntimeException throwAsUncheckedException(Throwable t) {
        if (t instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof IOException) {
            throw new UncheckedIOException(t.getMessage(), t);
        }
        throw new RuntimeException(t.getMessage(), t);
    }

    public static RuntimeException sneakyThrow(Throwable t) {
        throw Utils.<RuntimeException>sneakyThrow1(t);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneakyThrow1(Throwable t) throws E {
        throw (E)t;
    }
}
