rootProject.name = "jar-in-jar-mod"
include("gradle-plugin")
include("runtime-common")
include("runtime-fml-in-forge")
include("runtime-fml-in-cpw")

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
