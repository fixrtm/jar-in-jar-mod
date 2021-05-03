package com.anatawa12.jarInJar.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ExternalDependency;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.api.tasks.bundling.ZipEntryCompression;

import java.io.File;
import java.util.Objects;

import static com.anatawa12.jarInJar.gradle.Utils.callable;

public class JarInJarGradlePlugin implements Plugin<Project> {
    @SuppressWarnings("deprecation")
    @Override
    public void apply(Project target) {
        target.getConfigurations().create(RUNTIME_LIBRARY_CONFIGURATION);
        JarInJarExtension extension = target.getExtensions().create("jarInJar", JarInJarExtension.class);

        Jar jarTask = ((Jar) target.getTasks().getByName("jar"));

        Zip createStoredJar = target.getTasks().create("createStoredJar", Zip.class);
        createStoredJar.setEntryCompression(ZipEntryCompression.STORED);
        createStoredJar.dependsOn(extension.dependencyTasksGetter);
        createStoredJar.setDestinationDir(new File(target.getBuildDir(), "jarInJar"));
        createStoredJar.setArchiveName("stored-core.jar");
        createStoredJar.from(callable(() -> target.zipTree(jarTask.getArchivePath())));

        EmbedJarInJar embedJarInJar = target.getTasks().create("embedJarInJar", EmbedJarInJar.class);
        embedJarInJar.setSourceJar(callable(createStoredJar::getArchivePath));
        embedJarInJar.setDestinationJar(callable(jarTask::getArchivePath));
        embedJarInJar.dependsOn(createStoredJar);

        target.getConfigurations().all(config -> config.withDependencies(dependency ->
                dependency.stream().filter(it -> it instanceof ExternalDependency).map(ExternalDependency.class::cast)
                        .filter(it -> Objects.equals(it.getGroup(), "com.anatawa12.jarInJar")
                                && (it.getVersion() == null || it.getVersion().isEmpty()))
                        .forEach(it -> it.version(constraint -> constraint.require(CompileConstants.version)))));
    }

    public static final String RUNTIME_LIBRARY_CONFIGURATION = "jarInJarRuntimeLibrary";
}