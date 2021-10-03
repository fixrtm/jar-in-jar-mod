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
        metadataSources {
            artifact()
        }
    }
    maven("https://libraries.minecraft.net/") {
        name = "mojang"
    }
}

dependencies {
    //compileOnly("net.minecraftforge:forge:1.6.2-9.10.1.871:universal")
    compileOnly("net.minecraftforge:forge:1.7.10-10.13.4.1614-1.7.10:universal")
    compileOnly("net.minecraft:launchwrapper:1.12")
    compileOnly("org.apache.logging.log4j:log4j-api:2.0")
    compileOnly("org.ow2.asm:asm-debug-all:6.0_BETA")
    compileOnly("lzma:lzma:0.0.1")
    compileOnly(files(rootProject.tasks.createCompileTimeConstant.get().output)
        .builtBy(rootProject.tasks.createCompileTimeConstant))
    compileOnly(project(":runtime-common"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
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
    configure("runtime for fml in cpw package",
        "The runtime library for 1.6.2 ~ 1.7.10")
}
