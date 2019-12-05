import java.net.URL

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

description = "A Kotlin multiplatform STOMP client with JVM and JS support"

val serializationVersion = "0.14.0"

kotlin {
    jvm()
    js {
        nodejs()
        browser()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(project(":krossbow-engine-api"))

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation(project(":krossbow-engine-spring"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")

                val jacksonVersion = "2.9.9"
                implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation("uk.org.lidalia:slf4j-test:1.1.0")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation(project(":krossbow-engine-webstompjs"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serializationVersion")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

tasks.dokka {
    dependsOn(
        ":krossbow-engine-api:dokka",
        ":krossbow-engine-spring:dokka",
        ":krossbow-engine-webstompjs:dokka"
    )
    outputFormat = "javadoc"
    multiplatform {
        val global by creating {
            externalDocumentationLink {
                url = URL("file://${project(":krossbow-engine-api").buildDir}/dokka/krossbow-engine-api/")
                packageListUrl = URL(url, "package-list")
            }
        }
        val jvm by creating {
            externalDocumentationLink {
                url = URL("file://${project(":krossbow-engine-spring").buildDir}/dokka/krossbow-engine-spring/")
                packageListUrl = URL(url, "package-list")
            }
        }
        val js by creating {
            externalDocumentationLink {
                url = URL("file://${project(":krossbow-engine-webstompjs").buildDir}/dokka/krossbow-engine-webstompjs/")
                packageListUrl = URL(url, "package-list")
            }
        }
    }
}
