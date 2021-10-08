package com.anatawa12.jarInJar.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ExternalDependency;
import org.gradle.api.tasks.bundling.Jar;

import java.util.Objects;

import static com.anatawa12.jarInJar.gradle.internal.Utils.callable;

public class JarInJarGradlePlugin implements Plugin<Project> {
    @SuppressWarnings("deprecation")
    @Override
    public void apply(Project target) {
        target.apply((p) -> p.plugin("java"));

        target.getConfigurations().create(CREATOR_CONFIGURATION);
        target.getDependencies().add(CREATOR_CONFIGURATION, "com.anatawa12.jarInJar:jar-in-jar-creator");

        Jar jarTask = ((Jar) target.getTasks().getByName("jar"));

        EmbedJarInJar embedJarInJar = target.getTasks().create("embedJarInJar", EmbedJarInJar.class);
        embedJarInJar.setSourceJar(callable(jarTask::getArchivePath));
        embedJarInJar.setDestinationJar(callable(jarTask::getArchivePath));

        target.getConfigurations().all(config -> config.withDependencies(dependency ->
                dependency.stream().filter(it -> it instanceof ExternalDependency).map(ExternalDependency.class::cast)
                        .filter(it -> Objects.equals(it.getGroup(), "com.anatawa12.jarInJar")
                                && (it.getVersion() == null || it.getVersion().isEmpty()))
                        .forEach(it -> it.version(constraint -> constraint.require(CompileConstants.version)))));
    }

    public static final String CREATOR_CONFIGURATION = "jarInJarCreator";
}
