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
                implementation(npm("webstomp-client", "1.0.6"))
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
