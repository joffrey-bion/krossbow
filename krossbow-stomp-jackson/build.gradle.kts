import java.net.URL

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
}

tasks.dokka {
    dependsOn(":krossbow-stomp-core:dokka")
    configuration {
        externalDocumentationLink {
            url = URL("file://${project(":krossbow-stomp-core").buildDir}/dokka/krossbow-stomp-core/")
            packageListUrl = URL(url, "package-list")
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
