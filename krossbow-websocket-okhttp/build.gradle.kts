plugins {
    kotlin("jvm")
}

description = "A Krossbow adapter for OkHttp's WebSocket client"

dependencies {
    api(projects.krossbowWebsocketCore)
    api(libs.okhttp)
    implementation(libs.kotlinx.coroutines.jdk8)

    testImplementation(kotlin("test"))
    testImplementation(projects.krossbowWebsocketTest)
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
