package com.anatawa12.jarInJar.gradle;

import com.anatawa12.jarInJar.gradle.internal.DeferredUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.GUtil;

import java.io.File;

import static com.anatawa12.jarInJar.gradle.Utils.callable;

public class EmbedJarInJar extends DefaultTask {
    private Object creatorConfiguration = JarInJarGradlePlugin.CREATOR_CONFIGURATION;

    private Object keepFmlJsonCache = false;
    private Object target;
    private Object basePackage;
    private Object sourceJar;
    private Object destinationJar;
    private Object verbose = false;

    public EmbedJarInJar() {
        dependsOn(callable(this::getConfiguration));
    }

    //configurationForRuntime
    @Input
    public String getCreatorConfiguration() {
        return DeferredUtil.unpackString(creatorConfiguration, "configurationForRuntime");
    }

    public void setCreatorConfiguration(String creatorConfiguration) {
        this.creatorConfiguration = creatorConfiguration;
    }

    public void setCreatorConfiguration(Object creatorConfiguration) {
        this.creatorConfiguration = creatorConfiguration;
    }

    //keepFmlJsonCache
    @Input
    public boolean getKeepFmlJsonCache() {
        return DeferredUtil.unpackBoolean(keepFmlJsonCache);
    }

    public void setKeepFmlJsonCache(boolean keepFmlJsonCache) {
        this.keepFmlJsonCache = keepFmlJsonCache;
    }

    public void setKeepFmlJsonCache(Object keepFmlJsonCache) {
        this.keepFmlJsonCache = keepFmlJsonCache;
    }

    //target
    @Input
    public TargetPreset getTarget() {
        if (target instanceof TargetPreset) return (TargetPreset) target;
        String targetName = DeferredUtil.unpackString(target, "target");
        TargetPreset targetPreset = TargetPreset.byParamName(targetName);
        if (targetPreset != null) {
            return targetPreset;
        }
        return GUtil.toEnum(TargetPreset.class, targetName);
    }

    public void setTarget(boolean target) {
        this.target = target;
    }

    public void setTarget(TargetPreset target) {
        this.target = target;
    }

    //basePackage
    @Input
    @Optional
    public String getBasePackage() {
        return DeferredUtil.unpackString(basePackage, "basePackage", true);
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public void setBasePackage(Object basePackage) {
        this.basePackage = basePackage;
    }

    //sourceJar
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

    //verbose
    @Input
    public boolean getVerbose() {
        return DeferredUtil.unpackBoolean(verbose);
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setVerbose(Object verbose) {
        this.verbose = verbose;
    }

    @InputFiles
    public Configuration getConfiguration() {
        return getProject().getConfigurations().getByName(getCreatorConfiguration());
    }

    @TaskAction
    public void runTask() {
        getProject().javaexec((spec) -> {
            spec.classpath(getConfiguration());
            spec.setMain("com.anatawa12.jarInJar.creator.commandline.Main");
            spec.args("--cui");

            if (getVerbose())
                spec.args("--verbose");
            if (getKeepFmlJsonCache())
                spec.args("--verbose");

            String basePackage = getBasePackage();
            if (basePackage != null)
                spec.args("--base-package", basePackage);
            spec.args("--target", getTarget());
            spec.args("--input", getSourceJar());
            spec.args("--output", getDestinationJar());

            spec.setIgnoreExitValue(false);
        });
    }
}
