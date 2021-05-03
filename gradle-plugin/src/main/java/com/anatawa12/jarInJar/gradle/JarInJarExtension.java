package com.anatawa12.jarInJar.gradle;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

public abstract class JarInJarExtension {
    private Set<Object> dependencyTasks = new HashSet<>();

    public Set<Object> getDependencyTasks() {
        return dependencyTasks;
    }

    public void setDependencyTasks(Set<Object> dependencyTasks) {
        this.dependencyTasks = Objects.requireNonNull(dependencyTasks);
    }

    Callable<Object> dependencyTasksGetter = this::getDependencyTasks;
}
