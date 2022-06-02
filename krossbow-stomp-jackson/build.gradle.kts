plugins {
    kotlin("jvm")
}

description = "An extension of Krossbow STOMP client using Jackson for message conversions"

dependencies {
    api(projects.krossbowStompCore)
    api(libs.jackson.core)
    api(libs.jackson.module.kotlin)

    testImplementation(kotlin("test"))
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            artifact(sourcesJar)
        }
    }
}
