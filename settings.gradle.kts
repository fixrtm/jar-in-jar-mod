rootProject.name = "jar-in-jar-mod"
include("gradle-plugin")
include("runtime-fml-in-forge")

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
