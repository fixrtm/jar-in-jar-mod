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
    //compileOnly("net.minecraftforge:forge:1.12.2-14.23.5.2855:universal")
    compileOnly("net.minecraftforge:forge:1.8-11.14.4.1577:universal")
    compileOnly("net.minecraft:launchwrapper:1.12")
    compileOnly("org.apache.logging.log4j:log4j-api:2.8.1")
    compileOnly("org.ow2.asm:asm:6.0")
    compileOnly("lzma:lzma:0.0.1")
    compileOnly(files(rootProject.tasks.createCompileTimeConstant.get().output)
        .builtBy(rootProject.tasks.createCompileTimeConstant))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.test {
    useJUnitPlatform()
}

publishing.publications.create("maven", MavenPublication::class) {
    from(components["java"])
}
