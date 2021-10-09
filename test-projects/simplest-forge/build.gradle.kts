import com.anatawa12.jarInJar.gradle.TargetPreset

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath("com.anatawa12.jarInJar:gradle-plugin:+")
    }
}

apply {
    plugin("com.anatawa12.jarInJar")
}

tasks.embedJarInJar {
    target = TargetPreset.FMLInForge
    basePackage = "com.anatawa12.jarInJar.example.jarInJar"
}
tasks.assemble {
    dependsOn(tasks.embedJarInJar.get())
}
