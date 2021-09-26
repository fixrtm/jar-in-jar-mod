# Jar-in-Jar mod
[![a12 maintenance: Active](https://api.anatawa12.com/short/a12-active-svg)](https://api.anatawa12.com/short/a12-active-doc)
[![Maven Central](https://img.shields.io/maven-central/v/com.anatawa12.jarInJar/jar-in-jar-creator)](https://search.maven.org/search?q=g:com.anatawa12.jarInJar)
[![Maven metadata URL](https://img.shields.io/maven-metadata/v?label=Gradle%20Plugin%20Portal&logo=gradle&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fcom%2Fanatawa12%2FjarInJar%2Fcom.anatawa12.jarInJar.gradle.plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/com.anatawa12.jarInJar)

A trial to make a mod smaller

## How this work?

This creates a jar contains a lzma compressed non-compressing jar file and loader of the non-compressed jar mod.

## How to Use 

```groovy
import com.anatawa12.jarInJar.gradle.TargetPreset

plugins {
    id("com.anatawa12.jarInJar") version "version name here"
}

tasks.embedJarInJar {
    // choose one from two below by your mod's target forge version 
    target = TargetPreset.FMLInForge
    target = TargetPreset.FMLInCpw
    basePackage = "your.mod.package.name.jarInJar"
}
tasks.assemble {
    dependsOn tasks.embedJarInJar
    // or for kotlin
    // dependsOn(tasks.embedJarInJar.get())
}
```

## How much does this work?

[fixrtm-2.0.18] that was 5.34 MB is now 2.24 MB with this try.

[fixrtm-2.0.18]: https://www.curseforge.com/minecraft/mc-mods/fixrtm/files/3281913
