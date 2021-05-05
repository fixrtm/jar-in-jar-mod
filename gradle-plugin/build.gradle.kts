plugins {
    id("com.anatawa12.compile-time-constant")
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
    from(components["java"])
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
