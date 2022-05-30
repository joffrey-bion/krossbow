plugins {
    kotlin("jvm")
}

description = "An extension of Krossbow STOMP client using Moshi for message conversions"

dependencies {
    api(projects.krossbowStompCore)
    api(libs.moshi)
    testImplementation(kotlin("test"))
    testImplementation(libs.moshiKotlin) // for reflection-based serialization of Kotlin classes
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["kotlin"])
            artifact(sourcesJar)
        }
    }
}
