package com.anatawa12.jarInJar;

import java.io.File;

public class PostConstants {
    public static Class<?> CoreModManagerClass;
    public static Class<?> ModDiscovererClass;
    public static Class<?> ModCandidateClass;

    public static class ModCandidate {
        public ModCandidate(File classPathRoot, File modContainer, /*ContainerType*/ String sourceType) {
            throw new IllegalStateException();
        }
    }

    public static Object[] getFMLInjectionData() {
        throw new IllegalStateException();
    }

    public static String dottedFMLName(String inFML) {
        throw new IllegalStateException();
    }

    public static String slashedFMLName(String inFML) {
        throw new IllegalStateException();
    }
}
