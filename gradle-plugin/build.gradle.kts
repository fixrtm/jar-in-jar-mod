plugins {
    id("com.anatawa12.compile-time-constant")
    `maven-publish`
    `java-gradle-plugin`
    id("com.github.johnrengelman.shadow")
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    shadow(gradleApi())
    implementation("org.ow2.asm:asm:9.0")
    implementation("org.ow2.asm:asm-commons:9.0")
}

tasks.jar {
    isEnabled = false
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("org.objectweb.asm", "com.anatawa12.jarInJar.gradle.asm")
}

tasks.test {
    useJUnitPlatform()
}

tasks.createCompileTimeConstant {
    constantsClass = "com.anatawa12.jarInJar.gradle.CompileConstants"
    values(mapOf(
        "version" to version.toString(),
    ))
}

gradlePlugin.plugins.create("jarInJar") {
    implementationClass = "com.anatawa12.jarInJar.gradle.JarInJarGradlePlugin"
    id = "com.anatawa12.jarInJar"
}

publishing.publications.create("maven", MavenPublication::class) { 
    shadow.component(this)
}

tasks.withType<AbstractPublishToMaven>().configureEach {
    onlyIf {
        publication.name != "pluginMaven"
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf {
        publication.name != "jarInJarPluginMarkerMaven"
    }
}
