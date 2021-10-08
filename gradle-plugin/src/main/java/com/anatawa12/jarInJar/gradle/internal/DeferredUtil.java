package com.anatawa12.jarInJar.gradle.internal;

import kotlin.jvm.functions.Function0;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.provider.Provider;
import org.gradle.internal.Factory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.Callable;

public class DeferredUtil {
    public static Object unpack(@Nullable Object deferred) {
        if (unpackMethod != null) {
            try {
                return unpackMethod.invoke(null, deferred);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw Utils.sneakyThrow(e.getCause());
            }
        } else {
            return unpackFallback(deferred);
        }
    }

    public static String unpackString(Object value, String name) {
        return unpackString(value, name, false);
    }

    public static String unpackString(Object value, String name, boolean optional) {
        Object unpacked = unpack(value);
        if (!optional && unpacked == null) throw new IllegalStateException(name + " not yet set");
        if (unpacked == null) return null;
        return unpacked.toString();
    }

    public static boolean unpackBoolean(Object value) {
        Object unpacked = unpack(value);
        if (unpacked == null) return false;
        if (unpacked instanceof Boolean) return (Boolean) unpacked;
        if (unpacked instanceof String) return !((String) unpacked).isEmpty() || Boolean.parseBoolean((String) unpacked);
        if (unpacked instanceof Collection) return !((Collection<?>) unpacked).isEmpty();
        throw new ClassCastException("can't convert " + unpacked + " to boolean");
    }

    private final static Method unpackMethod;

    static {
        // because backwards compatibility
        //noinspection JavaReflectionMemberAccess
        unpackMethod = findMethod(
                // 3.3 -> 4.1
                () -> Class.forName("org.gradle.util.GFileUtils").getMethod("unpack", Object.class),
                // 4.2 -> 7.1
                () -> Class.forName("org.gradle.util.DeferredUtil").getMethod("unpack", Object.class),
                // 7.2 -> ??
                () -> Class.forName("org.gradle.util.internal.DeferredUtil").getMethod("unpack", Object.class)
        );
    }

    private interface MethodSupplier {
        Method get() throws ClassNotFoundException, NoSuchMethodException;
    }

    private static Method findMethod(MethodSupplier... methods) {
        for (MethodSupplier methodConsumer : methods) {
            Method method = null;
            try {
                method = methodConsumer.get();
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            }
            if (method != null) return method;
        }
        return null;
    }

    private static Object unpackFallback(@Nullable Object deferred) {
        if (deferred == null) {
            return null;
        }
        Object value = unpackNestableDeferred(deferred);
        if (value instanceof Provider) {
            return ((Provider<?>) value).get();
        }
        if (value instanceof Factory) {
            return ((Factory<?>) value).create();
        }
        return value;
    }

    private static Object unpackNestableDeferred(Object deferred) {
        Object current = deferred;
        while (true) {
            if (current instanceof Callable) {
                current = uncheckedCall((Callable<?>) current);
            } else if (current instanceof kotlin.jvm.functions.Function0<?>) {
                current = ((Function0<?>) current).invoke();
            } else {
                return current;
            }
        }
    }

    @Nullable
    public static <T> T uncheckedCall(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw Utils.throwAsUncheckedException(e);
        }
    }
}
