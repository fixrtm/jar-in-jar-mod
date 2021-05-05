import org.gradle.api.publish.maven.MavenPublication

fun MavenPublication.configure(componentName: String, desc: String) {
    pom {
        name.set("Jar In Jar Mod - $componentName")
        description.set("A plugin to create jar in jar. $desc")
        url.set("https://github.com/anatawa12/jar-in-jar-mod#readme")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://github.com/anatawa12/jar-in-jar-mod/blob/master/LICENSE.txt")
            }
        }
        developers {
            developer {
                id.set("anatawa12")
                name.set("anatawa12")
                email.set("anatawa12@icloud.com")
            }
        }
        scm {
            connection.set("scm:git:https://github.com/anatawa12/jar-in-jar-mod.git")
            developerConnection.set("scm:git:https://github.com/anatawa12/jar-in-jar-mod.git")
            url.set("https://github.com/anatawa12/jar-in-jar-mod.git")
        }
    }
}
