// import java.net.URL

plugins {
    kotlin("js")
}

description = "A Krossbow STOMP client JS implementation wrapping the Webstomp JS library"

kotlin {
    target {
        nodejs()
        browser()
    }
    sourceSets {
        main {
            dependencies {
                api(project(":krossbow-engine-api"))
                implementation(kotlin("stdlib-js"))
                implementation(npm("sockjs-client", "1.1.4"))
                implementation(npm("webstomp-client", "1.2.6"))
            }
        }
    }
}

tasks {
    compileKotlinJs {
        kotlinOptions.metaInfo = true
        kotlinOptions.sourceMap = true
        // commonjs module kind is necessary to use top-level declarations from UMD modules (e.g. react-redux)
        // because the Kotlin wrapper didn't specify @JsNonModule
        kotlinOptions.moduleKind = "commonjs"
        kotlinOptions.main = "call"
    }
    compileTestKotlinJs {
        // commonjs module kind is necessary to use top-level declarations from UMD modules (e.g. react-redux)
        // because the Kotlin wrapper didn't specify @JsNonModule
        kotlinOptions.moduleKind = "commonjs"
    }
}

tasks.dokka {
    enabled = false
//    dependsOn(":krossbow-engine-api:dokka")
//    configuration {
//        platform = "js"
//        externalDocumentationLink {
//            url = URL("file://${project(":krossbow-engine-api").buildDir}/dokka/krossbow-engine-api/")
//            packageListUrl = URL(url, "package-list")
//        }
//    }
}

val dokkaJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
//    from(tasks.dokka)
    from("doc/README.md")
}

val sourcesJar by tasks.creating(Jar::class) {
    from(kotlin.sourceSets["main"].kotlin)
    archiveClassifier.set("sources")
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
