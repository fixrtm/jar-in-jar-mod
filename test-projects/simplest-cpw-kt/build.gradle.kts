import com.anatawa12.jarInJar.gradle.TargetPreset

plugins {
    id("com.anatawa12.jarInJar")
}

repositories {
    mavenLocal()
    mavenCentral()
}

tasks.embedJarInJar {
    target = TargetPreset.FMLInCpw
    basePackage = "com.anatawa12.jarInJar.example.jarInJar"
}

tasks.assemble {
    dependsOn(tasks.embedJarInJar.get())
}

tasks.embedJarInJar {
    dependsOn(tasks.jar)
}
