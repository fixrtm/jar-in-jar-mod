package com.anatawa12.jarInJar.creator;

import com.anatawa12.jarInJar.creator.classPatch.ClassPatchParam;
import com.anatawa12.jarInJar.creator.classPatch.ClassPatcher;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.LZMAOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.anatawa12.jarInJar.creator.Constants.slashedLibraryBasePackage;
import static com.anatawa12.jarInJar.creator.Constants.slashedPostConstantsName;
import static com.anatawa12.jarInJar.creator.Utils.copyStream;
import static com.anatawa12.jarInJar.creator.Utils.createHash;
import static com.anatawa12.jarInJar.creator.Utils.makeSlashed;
import static com.anatawa12.jarInJar.creator.Utils.readStreamToByteArray;

public final class EmbedJarInJar {
    public InputStream input;
    public OutputStream destination;

    public String basePackage;
    public TargetPreset target;
    public EmbedProgressListener listener;

    public void runTask() throws IOException {
        createJarInJar(uncompressJar());
    }

    private File uncompressJar() throws IOException {
        listener.begin("Uncompress Jar");
        File temp = File.createTempFile("jar-in-jar-creator-uncompressed", ".jar");
        try (ZipInputStream zis = new ZipInputStream(input);
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(temp))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                zos.putNextEntry(new ZipEntry(entry.getName()));
                copyStream(zis, zos, false);
            }
        }
        listener.end();
        return temp;
    }

    private void createJarInJar(File sourceJarFile) throws IOException {
        listener.begin("Computing SHA256");
        byte[] sha256JarHash = createHash(new FileInputStream(sourceJarFile));

        if (basePackage == null) {
            basePackage = "com.anatawa12.jarInJar." + Utils.toHex(sha256JarHash);
        }

        listener.then("Writing zip");
        JarFile sourceJar = new JarFile(sourceJarFile);
        Manifest newJarManifest = createManifest(sourceJar);

        try (ZipOutputStream out = new ZipOutputStream(destination)) {
            listener.begin("Writing MANIFEST.MF");
            out.setLevel(Deflater.BEST_COMPRESSION);
            // write manifest
            out.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            newJarManifest.write(out);

            listener.then("Copying Access Transformer Config");
            // copy access transformer contents
            String atList = newJarManifest.getMainAttributes().getValue(FMLAT);
            copyAccessTransformerConfigs(atList, sourceJar, out);

            listener.then("Compressing and Copying LZMA");
            // write main content
            out.putNextEntry(new ZipEntry("core.jar.lzma"));
            LZMAOutputStream lzmaOut = new LZMAOutputStream(out,
                    new LZMA2Options(), -1L);
            copyStream(new FileInputStream(sourceJarFile), lzmaOut, true);
            lzmaOut.finish();

            listener.then("Transforming and Copying Runtime Library");
            // copy runtime contents
            copyRuntimeContents(out, sha256JarHash);

            listener.then("Copying Runtime core.mf");
            // write manifest
            out.putNextEntry(new ZipEntry("core.mf"));
            sourceJar.getManifest().write(out);

            listener.end();
        }
        listener.end();
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

    private void copyAccessTransformerConfigs(String atList, JarFile sourceJar, ZipOutputStream out) throws IOException {
        if (atList == null) return;

        for (String at : atList.split(" ")) {
            JarEntry jarEntry = sourceJar.getJarEntry("META-INF/" + at);
            if (jarEntry == null) continue;
            out.putNextEntry(new ZipEntry(jarEntry.getName()));
            copyStream(sourceJar.getInputStream(jarEntry), out, true);
        }
    }

    private InputStream getInputStream(String name) {
        InputStream stream = EmbedJarInJar.class.getResourceAsStream(name);
        if (stream == null)
            throw new IllegalStateException("embedded resource "  + name + " not found.");        return stream;
    }

    private void copyRuntimeContents(ZipOutputStream out, byte[] sha256JarHash) throws IOException {
        ClassPatchParam param = new ClassPatchParam(makeSlashed(basePackage), target, sha256JarHash);
        try (ZipInputStream runtimeCommon = new ZipInputStream(getInputStream("/runtime-common.jar"))) {
            copyJarWithPackageRenaming(out, runtimeCommon, param);
        }
        try (ZipInputStream runtimeCommon = new ZipInputStream(getInputStream("/runtime-" + target.jarName + ".jar"))) {
            copyJarWithPackageRenaming(out, runtimeCommon, param);
        }
    }

    private void copyJarWithPackageRenaming(ZipOutputStream out, ZipInputStream runtimeCommon, ClassPatchParam param) throws IOException {

        ZipEntry entry;
        while ((entry = runtimeCommon.getNextEntry()) != null) {
            if (!entry.getName().startsWith(slashedLibraryBasePackage))
                continue;
            if (entry.getName().startsWith(slashedPostConstantsName))
                continue;

            listener.begin("Copying " + entry.getName());
            String newName = param.slashedBasePackage + entry.getName().substring(slashedLibraryBasePackage.length());

            out.putNextEntry(new ZipEntry(newName));
            if (entry.getName().endsWith(".class")) {
                out.write(ClassPatcher.modify(readStreamToByteArray(runtimeCommon), param));
            } else {
                copyStream(runtimeCommon, out, false);
            }
            listener.end();
        }
    }

    private static final Attributes.Name FMLCorePlugin =  new Attributes.Name("FMLCorePlugin");
    private static final Attributes.Name FMLAT = new Attributes.Name("FMLAT");
    private static final Attributes.Name MODSIDE = new Attributes.Name("ModSide");
}
