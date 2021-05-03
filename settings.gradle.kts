rootProject.name = "jar-in-jar-mod"
include("gradle-plugin")
include("runtime")

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
