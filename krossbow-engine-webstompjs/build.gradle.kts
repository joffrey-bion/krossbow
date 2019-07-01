import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    kotlin("js")
}

description = "A Krossbow STOMP client JS implementation wrapping the Webstomp JS library"

repositories {
    jcenter()
}

kotlin {
    target {
        nodejs()
        browser()
    }
    sourceSets["main"].dependencies {
        api(project(":krossbow-engine-api"))
        implementation(kotlin("stdlib-js"))
        implementation(npm("sockjs-client", "1.1.4"))
        implementation(npm("webstomp-client", "1.0.6"))
    }
}

tasks {
    "compileKotlinJs"(Kotlin2JsCompile::class) {
        kotlinOptions.metaInfo = true
        kotlinOptions.outputFile = "${project.buildDir.path}/js/${project.name}.js"
        kotlinOptions.sourceMap = true
        kotlinOptions.moduleKind = "commonjs"
        kotlinOptions.main = "noCall"
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
