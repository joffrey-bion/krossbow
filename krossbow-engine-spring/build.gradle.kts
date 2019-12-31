import java.net.URL

plugins {
    kotlin("jvm")
}

description = "A Krossbow STOMP client JVM implementation wrapping the Spring WebsocketStompClient"

dependencies {
    api(project(":krossbow-engine-api"))

    api("org.slf4j:slf4j-api:1.7.26")

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect")) // required by jackson-module-kotlin

    // For Spring's STOMP client
    val springVersion = "5.1.7.RELEASE"
    implementation("org.springframework:spring-websocket:$springVersion")
    implementation("org.springframework:spring-messaging:$springVersion")

    // JSR 356 - Java API for WebSocket (reference implementation)
    // Low-level implementation required by Spring's client
    implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client-jdk:1.15")

    val jacksonVersion = "2.9.9"
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
}

tasks.dokka {
    dependsOn(":krossbow-engine-api:dokka")
    configuration {
        externalDocumentationLink {
            url = URL("https://docs.spring.io/spring/docs/current/javadoc-api/")
            packageListUrl = URL(url, "package-list")
        }
        // externalDocumentationLink {
        //     url = URL("file://${project(":krossbow-engine-api").buildDir}/dokka/krossbow-engine-api/")
        //     packageListUrl = URL(url, "package-list")
        // }
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
