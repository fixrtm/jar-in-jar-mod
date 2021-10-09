pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.anatawa12.jarInJar")
                useModule("com.anatawa12.jarInJar:gradle-plugin:${System.getenv("PLUGIN_VERSION")}")
        }
    }
}
