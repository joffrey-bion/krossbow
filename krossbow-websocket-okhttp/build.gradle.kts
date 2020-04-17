plugins {
    kotlin("jvm")
}

description = "A Krossbow adapter for OkHttp's WebSocket client"

dependencies {
    api(project(":krossbow-websocket-core"))

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.coroutines}")

    api("com.squareup.okhttp3:okhttp:4.5.0")
}

tasks.dokka {
    dependsOn(":krossbow-websocket-core:dokka")
    configuration {
        externalDocumentationLink {
            url = relativeDokkaUrl("krossbow-websocket-core")
            packageListUrl = relativeDokkaPackageListUrl("krossbow-websocket-core")
        }
    }
}

val dokkaJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.dokka)
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
            artifact(dokkaJar)
            artifact(sourcesJar)
        }
    }
}
