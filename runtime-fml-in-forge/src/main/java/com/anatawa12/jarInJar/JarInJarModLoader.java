package com.anatawa12.jarInJar;

import LZMA.LzmaInputStream;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.discovery.ContainerType;
import net.minecraftforge.fml.common.discovery.ModCandidate;
import net.minecraftforge.fml.common.discovery.ModDiscoverer;
import net.minecraftforge.fml.relauncher.CoreModManager;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.anatawa12.jarInJar.CompileConstants.day;
import static com.anatawa12.jarInJar.CompileConstants.hour;
import static com.anatawa12.jarInJar.CompileConstants.major;
import static com.anatawa12.jarInJar.CompileConstants.minor;
import static com.anatawa12.jarInJar.CompileConstants.minute;
import static com.anatawa12.jarInJar.CompileConstants.patch;

public class JarInJarModLoader {
    @SuppressWarnings({"PointlessArithmeticExpression"})
    public static final long revision = 1_000_000_0_00000_00_00L
            + (10000000000000L * minor)
            + (10000000000L * patch)
            + (10000L * day)
            + (100L * hour)
            + (1L * minute);

    public static final String versionName = day == 999 && hour == 99 && patch == 99
            ? "" + major + '.' + minor + '.' + patch
            : "" + major + '.' + minor + '.' + patch
            + "-SNAPSHOT-" + day + "-" + hour + "-" + minute;

    private static final Logger LOGGER = LogManager.getLogger("JarInJarModLoader-" + versionName);

    public static void load() {
        checkVersion();
        LOGGER.info("loading JarInJar by " + JarInJarModLoader.class.getName());

        ((LaunchClassLoader) JarInJarModLoader.class.getClassLoader()).addClassLoaderExclusion("com.sun.");
        Path jar;
        try {
            URI uri = JarInJarModLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            if (uri.getScheme().equals("jar"))
                uri = new URI(uri.toString().substring("jar:".length(), uri.toString().indexOf("!/")));
            jar = Paths.get(uri);
            System.out.println("jar: " + jar);
            if (!jar.toString().endsWith(".jar") && !jar.toString().endsWith(".zip"))
                throw new IllegalStateException("jar-in-jar must contain in jar or zip file");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        String key = null;
        try {
            key = minecraftDir.relativize(jar).toString();
        } catch (Throwable ignored) {
        }
        if (key == null || key.startsWith(".."))
            key = sha256(jar.getParent().toString()) + File.separatorChar + jar.getName(jar.getNameCount() - 1);
        getModsSet().add(key);
        Path extracted = cacheJar(key, jar, getSha256Digest());
        loadCoreModIfNecessary(key, jar, extracted);
    }

    private static void loadCoreModIfNecessary(String key, Path jar, Path extracted) {
        Attributes mfAttributes;
        try (ZipFile file = new ZipFile(jar.toFile())) {
            ZipEntry manifestEntry = file.getEntry("core.mf");
            if (manifestEntry == null) return;
            try (InputStream stream = file.getInputStream(manifestEntry)) {
                mfAttributes = new Manifest(stream).getMainAttributes();
            }
        } catch (IOException ignored) {
            return;
        }
        checkAndLoadCoreMod(mfAttributes, key, jar.getName(jar.getNameCount() - 1).toString(), extracted);
    }

    private static void checkAndLoadCoreMod(
            Attributes mfAttributes,
            String key,
            String jarName,
            Path extracted
    ) {
        String fmlCorePlugin = mfAttributes.getValue("FMLCorePlugin");
        if (fmlCorePlugin == null) {
            // Not a coremod
            LOGGER.debug("Not found coremod data in {}", jarName);
            return;
        }
        LaunchClassLoader classLoader = (LaunchClassLoader) JarInJarModLoader.class.getClassLoader();
        try {
            classLoader.addURL(extracted.toUri().toURL());
        } catch (MalformedURLException e) {
            LOGGER.error("Unable to convert file into a URL. weird", e);
            return;
        }
        if (!mfAttributes.containsKey("FMLCorePluginContainsFMLMod")) {
            LOGGER.trace("Adding {} to the list of known coremods, it will not be examined again", jarName);
            getIgnoredSet().add(key);
        } else {
            LOGGER.warn("Found FMLCorePluginContainsFMLMod marker in {}. This is not recommended by FML, " +
                            "@Mods should be in a separate jar from the coremod.",
                    jarName);
        }
        try {
            Method loadCoreMod = CoreModManager.class.getDeclaredMethod("loadCoreMod",
                    LaunchClassLoader.class, String.class, File.class);
            loadCoreMod.setAccessible(true);
            for (String className : fmlCorePlugin.split(";")) {
                if (className.isEmpty()) continue;
                loadCoreMod.invoke(null, classLoader, className, extracted.toFile());
            }
        } catch (IllegalAccessException|NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw handleInvocationTargetException(e);
        }
    }

    private static RuntimeException handleInvocationTargetException(InvocationTargetException e) {
        Throwable t = e.getTargetException();
        if (t instanceof RuntimeException) throw (RuntimeException) t;
        if (t instanceof Error) throw (Error) t;
        throw new RuntimeException(t);
    }

    private static String sha256(String parent) {
        MessageDigest sha256Digest = getSha256Digest();
        return toHex(sha256Digest.digest(parent.getBytes(StandardCharsets.UTF_8)));
    }

    private static String toHex(byte[] data) {
        char[] hash = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            hash[i * 2] = "0123456789abcdef".charAt((data[i] >> 4) & 0xF);
            hash[i * 2 + 1] = "0123456789abcdef".charAt(data[i] & 0xF);
        }
        return new String(hash);
    }

    // will be called by transformed class
    @SuppressWarnings("unused")
    public static void identifyMods(ModDiscoverer discoverer) {
        LOGGER.info("identifyMods JarInJar by " + JarInJarModLoader.class.getName());
        Set<Path> filesInDir;
        try {
            filesInDir = Files.walk(cacheDir)
                    .filter(Files::isRegularFile)
                    .map(cacheDir::relativize)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
            filesInDir = Collections.emptySet();
        }

        Set<String> modsMap = getModsSet();
        LOGGER.info("mods will be loaded: " + modsMap);
        // removeIf for emptySet no effect
        //noinspection ConstantConditions
        filesInDir.removeIf((path) -> modsMap.contains(path.toString()));
        LOGGER.info("deleting unused files " + filesInDir);

        for (Path path : filesInDir)
            //noinspection ResultOfMethodCallIgnored
            cacheDir.resolve(path).toFile().delete();

        try {
            Method addCandidate = ModDiscoverer.class.getDeclaredMethod("addCandidate", ModCandidate.class);
            addCandidate.setAccessible(true);
            MessageDigest sha256Digest = getSha256Digest();
            for (String aModInfo : modsMap) {
                File jarFileFile = cacheDir.resolve(aModInfo).toFile();

                addCandidate.invoke(discoverer, new ModCandidate(jarFileFile, jarFileFile, ContainerType.JAR));
            }
        } catch (NoSuchMethodException|IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw handleInvocationTargetException(e);
        }
    }

    private static final Path minecraftDir = ((File) FMLInjectionData.data()[6]).toPath();
    private static final Path cacheDir = minecraftDir.resolve("jar-mods-cache").resolve("v1");

    private static MessageDigest getSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static Set<String> getModsSet() {
        @SuppressWarnings("unchecked")
        Set<String> value = (Set<String>) Launch.blackboard.get(modsBlackboardKey);
        if (value == null) {
            value = new HashSet<>();
            Launch.blackboard.put(modsBlackboardKey, value);
        }
        return value;
    }

    private static Set<String> getIgnoredSet() {
        @SuppressWarnings("unchecked")
        Set<String> value = (Set<String>) Launch.blackboard.get(ignoredBlackboardKey);
        if (value == null) {
            value = new HashSet<>();
            Launch.blackboard.put(ignoredBlackboardKey, value);
        }
        return value;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void checkVersion() {
        Class clazz = (Class) Launch.blackboard.get(loaderBlackboardKey);
        if (clazz == null || ObfuscationReflectionHelper
                .<Long, Object>getPrivateValue(clazz, null, "revision") < revision)
            Launch.blackboard.put(loaderBlackboardKey, JarInJarModLoader.class);
    }

    public static boolean isLatest() {
        return Launch.blackboard.get(loaderBlackboardKey) == JarInJarModLoader.class;
    }

    // region cacheJar
    private static Path cacheJar(String key, Path modJar, MessageDigest sha256Digest) {
        try {
            Path extractedFile = cacheDir.resolve(key);

            try (ZipFile jarFile = new ZipFile(modJar.toFile())) {
                ZipEntry jarEntry = jarFile.getEntry("core.jar.lzma");
                ZipEntry sha256Entry = jarFile.getEntry("core.sha256");
                byte[] sha256 = readSha256File(jarFile, sha256Entry);

                // if cache mismatch, write
                if (!Files.exists(extractedFile) || !checkCache(extractedFile, jarEntry, sha256, sha256Digest)) {
                    sha256Digest.reset();
                    Files.createDirectories(extractedFile.getParent());
                    // if not: write to cache and verify hash
                    try (
                            InputStream jarReading = new LzmaInputStream(jarFile.getInputStream(jarEntry));
                            OutputStream jarWriting = Files.newOutputStream(extractedFile);
                    ) {
                        byte[] buf = new byte[1024];
                        int read;
                        while ((read = jarReading.read(buf)) != -1) {
                            sha256Digest.update(buf, 0, read);
                            jarWriting.write(buf, 0, read);
                        }
                    }

                    if (!Arrays.equals(sha256Digest.digest(), sha256))
                        throw new IllegalStateException("reading invalid jar-in-jar: hash mismatch: " + modJar);
                }
            }

            return extractedFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] readSha256File(ZipFile jarFile, ZipEntry sha256Entry) throws IOException {
        byte[] chars = new byte[64];
        try (InputStream stream = jarFile.getInputStream(sha256Entry)) {
            if (stream.read(chars) != 64)
                throw new IOException(jarFile.getName() + " is a invalid jar-in-jar mod: invalid size of sha256");
        }
        byte[] data = new byte[chars.length / 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (hex2bin(chars[i * 2]) << 4 | hex2bin(chars[i * 2 + 1]));
        }
        return data;
    }

    private static byte hex2bin(byte aChar) {
        if ('0' <= aChar && aChar <= '9') return (byte) (aChar - '0');
        if ('a' <= aChar && aChar <= 'f') return (byte) (aChar - 'a' + 10);
        if ('A' <= aChar && aChar <= 'F') return (byte) (aChar - 'A' + 10);
        throw new IllegalArgumentException("invalid hex: " + (char) (aChar & 0xFF));
    }

    @SuppressWarnings("RedundantIfStatement")
    private static boolean checkCache(Path extracted, ZipEntry jarEntry, byte[] sha256, MessageDigest sha256Digest) throws IOException {
        if (Files.size(extracted) != jarEntry.getSize()) return false;

        sha256Digest.reset();

        try (InputStream extractedStream = Files.newInputStream(extracted)) {
            byte[] buf = new byte[1024];
            int read;
            while ((read = extractedStream.read(buf)) != -1) {
                sha256Digest.update(buf, 0, read);
            }
        }

        // if sha256 mismatch, invalid cache.
        if (!Arrays.equals(sha256Digest.digest(), sha256)) return false;

        return true;
    }
    // endregion

    private static final String modsBlackboardKey = "com.anatawa12.jarInJar.JarInJarModLoader.v1.mods";
    private static final String ignoredBlackboardKey = "com.anatawa12.jarInJar.JarInJarModLoader.v1.ignored";
    private static final String loaderBlackboardKey = "com.anatawa12.jarInJar.JarInJarModLoader.v1.value";
}
