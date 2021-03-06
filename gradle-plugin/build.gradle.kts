plugins {
    id("com.anatawa12.compile-time-constant")
    id("com.gradle.plugin-publish")
    `maven-publish`
    `java-gradle-plugin`
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
}

java {
    withJavadocJar()
    withSourcesJar()
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
    displayName = "Jar In Jar Mod"
    description = "A plugin to create a mod jar smaller."
    implementationClass = "com.anatawa12.jarInJar.gradle.JarInJarGradlePlugin"
    id = "com.anatawa12.jarInJar"
}

pluginBundle {
    website = "https://github.com/anatawa12/jar-in-jar-mod"
    vcsUrl = "https://github.com/anatawa12/jar-in-jar-mod.git"
    tags = listOf("minecraft")
    mavenCoordinates {
        groupId = project.group.toString()
    }
}

publishing.publications.create("maven", MavenPublication::class) {
    from(components["java"])
    configure("Gradle Plugin", 
        "The Gradle Plugin to create jar-in-jar mod with Creator")
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
