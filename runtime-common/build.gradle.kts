plugins {
    java
    `maven-publish`
    id("com.anatawa12.compile-time-constant")
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
    maven(url = "https://maven.minecraftforge.net/") {
        name = "forge"
    }
    maven("https://libraries.minecraft.net/") {
        name = "mojang"
    }
}

dependencies {
    compileOnly("net.minecraft:launchwrapper:1.12")
    compileOnly("org.apache.logging.log4j:log4j-api:2.0")
    compileOnly("org.ow2.asm:asm:6.0")
    compileOnly("lzma:lzma:0.0.1")
    compileOnly(files(rootProject.tasks.createCompileTimeConstant.get().output)
        .builtBy(rootProject.tasks.createCompileTimeConstant))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}

publishing.publications.create("maven", MavenPublication::class) {
    from(components["java"])
    configure("runtime for launch wrapper based versions",
        "The runtime library for 1.6.2 ~ 1.12.2")
}
