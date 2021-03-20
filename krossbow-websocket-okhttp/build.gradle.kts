plugins {
    kotlin("jvm")
}

description = "A Krossbow adapter for OkHttp's WebSocket client"

dependencies {
    api(project(":krossbow-websocket-core"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.coroutines}")

    api("com.squareup.okhttp3:okhttp:4.9.0")
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
