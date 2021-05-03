package com.anatawa12.jarInJar.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.DeferredUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.LZMAOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.anatawa12.jarInJar.gradle.Utils.callable;
import static com.anatawa12.jarInJar.gradle.Utils.copyStream;

public class EmbedJarInJar extends DefaultTask {
    private Object sourceJar;
    private Object destinationJar;
    private Object basePackage;
    private Object configurationForRuntime = JarInJarGradlePlugin.RUNTIME_LIBRARY_CONFIGURATION;
    private Object runtimeLibraryBasePackage = "com.anatawa12.jarInJar";

    public EmbedJarInJar() {
        dependsOn(callable(this::getConfiguration));
    }

    @InputFile
    public File getSourceJar() {
        return getProject().file(sourceJar);
    }

    public void setSourceJar(File sourceJar) {
        this.sourceJar = sourceJar;
    }

    public void setSourceJar(Object sourceJar) {
        this.sourceJar = sourceJar;
    }

    @OutputFile
    public File getDestinationJar() {
        return getProject().file(destinationJar);
    }

    public void setDestinationJar(File destinationJar) {
        this.destinationJar = destinationJar;
    }

    public void setDestinationJar(Object destinationJar) {
        this.destinationJar = destinationJar;
    }

    private String unpackString(Object value, String name) {
        Object unpacked = DeferredUtil.unpack(value);
        if (unpacked == null) throw new IllegalStateException(name + " not yet set");
        return unpacked.toString();
    }

    @Input
    public String getBasePackage() {
        return unpackString(basePackage, "basePackage");
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    @Input
    public String getConfigurationForRuntime() {
        return unpackString(configurationForRuntime, "configurationForRuntime");
    }

    public void setConfigurationForRuntime(String configurationForRuntime) {
        this.configurationForRuntime = configurationForRuntime;
    }

    @Input
    public String getRuntimeLibraryBasePackage() {
        return unpackString(runtimeLibraryBasePackage, "runtimeLibraryBasePackage");
    }

    public void setRuntimeLibraryBasePackage(String runtimeLibraryBasePackage) {
        this.runtimeLibraryBasePackage = runtimeLibraryBasePackage;
    }

    private Configuration getConfiguration() {
        return getProject().getConfigurations().getByName(getConfigurationForRuntime());
    }

    @InputFile
    public File getRuntimeJar() {
        Configuration config = getConfiguration();
        Set<File> files = config.getFiles();
        if (files.isEmpty())
            throw new IllegalStateException("no jar-in-jar runtime was configured for " + config + "\n" +
                    "Please add `com.anatawa12.jarInJar:runtime-fml-in-cpw` for 1.7.10 or earlier or \n" +
                    "`com.anatawa12.jarInJar:runtime-fml-in-forge` for 1.8 or older.");
        if (files.size() != 1)
            throw new IllegalStateException("multiple jar-in-jar runtime was configured for " + config);
        return files.iterator().next();
    }

    private String slashedLibraryBasePackage;
    private String slashedBasePackage;

    @TaskAction
    public void runTask() throws IOException {
        JarFile runtimeJar = new JarFile(getRuntimeJar());
        File sourceJarFile = getSourceJar();
        JarFile sourceJar = new JarFile(sourceJarFile);

        slashedLibraryBasePackage = makeSlashed(getRuntimeLibraryBasePackage());
        slashedBasePackage = makeSlashed(getBasePackage());

        Manifest newJarManifest = createManifest(sourceJar);

        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(getDestinationJar()))) {
            out.setLevel(Deflater.BEST_COMPRESSION);
            // write manifest
            out.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            newJarManifest.write(out);

            // copy runtime contents
            copyRuntimeContents(runtimeJar, out);

            // copy access transformer contents
            String atList = newJarManifest.getMainAttributes().getValue(FMLAT);
            copyAccessTransformerConfigs(atList, sourceJar, out);

            // write main content
            out.putNextEntry(new ZipEntry("core.jar.lzma"));
            LZMAOutputStream lzmaOut = new LZMAOutputStream(out,
                    new LZMA2Options(), -1L);
            copyStream(new FileInputStream(sourceJarFile), lzmaOut);
            lzmaOut.finish();

            out.putNextEntry(new ZipEntry("core.sha256"));
            createHash(new FileInputStream(sourceJarFile), out);

            // write manifest
            out.putNextEntry(new ZipEntry("core.mf"));
            sourceJar.getManifest().write(out);
        }
    }

    private void createHash(FileInputStream inputStream, OutputStream out) throws IOException {
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
        byte[] file = new byte[hash.length * 2 + 1];
        for (int i = 0; i < hash.length; i++) {
            file[i * 2] = (byte) "0123456789abcdef".charAt((hash[i] >> 4) & 0xF);
            file[i * 2 + 1] = (byte) "0123456789abcdef".charAt(hash[i] & 0xF);
        }
        file[file.length - 1] = '\n';
        out.write(file);
    }

    private void copyAccessTransformerConfigs(String atList, JarFile sourceJar, ZipOutputStream out) throws IOException {
        if (atList == null) return;

        for (String at : atList.split(" ")) {
            JarEntry jarEntry = sourceJar.getJarEntry("META-INF/" + at);
            if (jarEntry == null) continue;
            out.putNextEntry(new ZipEntry(jarEntry.getName()));
            copyStream(sourceJar.getInputStream(jarEntry), out);
        }
    }

    private String makeSlashed(String dotted) {
        String slashed = dotted.replace('.', '/');
        if (slashed.contains("//")) throw new IllegalStateException("package name has .. part");
        if (!slashed.endsWith("/")) slashed = slashed + '/';
        return slashed;
    }

    private void copyRuntimeContents(JarFile runtimeJar, ZipOutputStream out) throws IOException {
        for (Enumeration<JarEntry> entries = runtimeJar.entries(); entries.hasMoreElements();) {
            JarEntry entry = entries.nextElement();

            if (!entry.getName().startsWith(slashedLibraryBasePackage))
                continue;
            String newName = packageRemapper.map(entry.getName());

            out.putNextEntry(new ZipEntry(newName));
            if (entry.getName().endsWith(".class")) {
                ClassReader reader;
                try (InputStream inputStream = runtimeJar.getInputStream(entry)) {
                    reader = new ClassReader(inputStream);
                }
                ClassWriter writer = new ClassWriter(0);
                reader.accept(new ClassRemapper(writer, packageRemapper), 0);
                out.write(writer.toByteArray());
            } else {
                copyStream(runtimeJar.getInputStream(entry), out);
            }
        }
    }

    private Manifest createManifest(JarFile sourceJar) throws IOException {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        Attributes sourceAttr = sourceJar.getManifest().getMainAttributes();

        attributes.putValue("Manifest-Version", "1.0");
        attributes.put(FMLCorePlugin, basePackage + ".JarInJarLoaderCoreMod");

        // copy ModSide and FMLAT and FMLCorePluginContainsFMLMod
        if (sourceAttr.getValue(MODSIDE) != null)
            attributes.put(MODSIDE, sourceAttr.getValue(MODSIDE));

        if (sourceAttr.getValue(FMLAT) != null)
            attributes.put(FMLAT, sourceAttr.getValue(FMLAT));

        return manifest;
    }

    private static final Attributes.Name FMLCorePlugin =  new Attributes.Name("FMLCorePlugin");
    private static final Attributes.Name FMLAT = new Attributes.Name("FMLAT");
    private static final Attributes.Name MODSIDE = new Attributes.Name("ModSide");

    private final PackageRemapper packageRemapper = new PackageRemapper();

    private class PackageRemapper extends Remapper {
        @Override
        public String map(String internalName) {
            if (!internalName.startsWith(slashedLibraryBasePackage))
                return internalName;
            return slashedBasePackage + internalName.substring(slashedLibraryBasePackage.length());
        }
    }
}
