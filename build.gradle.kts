import java.time.*

plugins {
    java
    `maven-publish`
    id("com.anatawa12.compile-time-constant") version "1.0.4"
    id("com.github.johnrengelman.shadow") version "7.0.0" apply false
}

val major = 1
val minor = 0
val patch = 0
val snapshot = true
val epoc = LocalDate.of(2021, 4, 29)!!

group = "com.anatawa12.jarInJar"
version = "$major.$minor.$patch" + if (snapshot) "-SNAPSHOT" else ""

val dhm = if (!snapshot) Triple(99999, 99, 99) else {
    val date = LocalDate.now(ZoneOffset.UTC)
    val range = epoc.until(date)
    val time = LocalDateTime.now(ZoneOffset.UTC)

    if (range.days <= 99999) {
        Triple(range.days, time.hour, time.minute)
    } else {
        System.err.println("day out of range")
        Triple(99999, time.hour, time.minute)
    }
}

val day: Int = dhm.first
val hour: Int = dhm.second
val minute: Int = dhm.third

tasks.createCompileTimeConstant {
    constantsClass = "com.anatawa12.jarInJar.CompileConstants"
    values(mapOf(
        "epocYear" to epoc.year,
        "epocMon" to epoc.monthValue,
        "epocDay" to epoc.dayOfMonth,
        "major" to major,
        "minor" to minor,
        "patch" to patch,
        "day" to day,
        "hour" to hour,
        "minute" to minute,
    ))
}

subprojects {
    apply(plugin = "maven-publish")

    publishing.repositories.maven {
        name = "sonatype-oss-rh"
        url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")

        credentials {
            username = project.findProperty("com.anatawa12.sonatype.username")?.toString() ?: ""
            password = project.findProperty("com.anatawa12.sonatype.passeord")?.toString() ?: ""
        }
    }
}
