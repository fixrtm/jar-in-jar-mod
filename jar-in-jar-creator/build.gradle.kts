plugins {
    java
    application
    `maven-publish`
    id("com.github.johnrengelman.shadow")
    id("com.anatawa12.compile-time-constant")
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.tukaani:xz:1.9")
    implementation("org.ow2.asm:asm:9.4")
    implementation("org.ow2.asm:asm-commons:9.2")
}

java {
    withJavadocJar()
    withSourcesJar()
}

application {
    mainClass.set("com.anatawa12.jarInJar.creator.commandline.Main")
}

tasks.jar {
    isEnabled = false
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("org.objectweb.asm", "com.anatawa12.jarInJar.creator.asm")
    relocate("org.tukaani.xz", "com.anatawa12.jarInJar.creator.xz")
}

tasks.createCompileTimeConstant {
    constantsClass = "com.anatawa12.jarInJar.creator.CompileConstants"
    values(mapOf(
        "version" to version.toString(),
    ))
}

tasks.processResources {
    from("..") {
        include("LICENSE.txt")
    }
    from(project(":runtime-common").tasks.getByName("jar")) {
        rename { "runtime-common.jar.bin" }
    }
    from(project(":runtime-fml-in-cpw").tasks.getByName("jar")) {
        rename { "runtime-fml-in-cpw.jar.bin" }
    }
    from(project(":runtime-fml-in-forge").tasks.getByName("jar")) {
        rename { "runtime-fml-in-forge.jar.bin" }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}

publishing.publications.create("maven", MavenPublication::class) { 
    shadow.component(this)
    artifact(project.tasks.named("javadocJar"))
    artifact(project.tasks.named("sourcesJar"))
    configure("Creator", "jar-in-jar mod creating tool.")
}
