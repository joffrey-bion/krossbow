plugins {
    kotlin("jvm")
}

description = "An extension of Krossbow STOMP client using Jackson for message conversions"

val jacksonVersion = "2.10.0"

dependencies {
    api(project(":krossbow-stomp-core"))
    implementation(kotlin("stdlib-jdk8"))
    api("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

tasks.dokka {
    dependsOn(":krossbow-stomp-core:dokka")
    configuration {
        externalDocumentationLink {
            url = relativeDokkaUrl("krossbow-stomp-core")
            packageListUrl = relativeDokkaPackageListUrl("krossbow-stomp-core")
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
