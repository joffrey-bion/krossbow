plugins {
    val kotlinVersion = "1.6.21"
    kotlin("jvm") apply false
    kotlin("js") apply false
    kotlin("multiplatform") apply false
    kotlin("plugin.spring") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false
    id("org.jetbrains.dokka") version "1.6.20" apply false
    id("org.hildan.github.changelog") version "1.11.1"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.8.0"
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
    id("com.louiscad.complete-kotlin") version "1.1.0" // for autocomplete of Apple libraries on non-macOS systems
}

allprojects {
    group = "org.hildan.krossbow"

    repositories {
        mavenCentral()
    }

    apply(plugin = "org.jetbrains.dokka")

    afterEvaluate {
        tasks.withType<org.jetbrains.dokka.gradle.DokkaMultiModuleTask> {
            outputDirectory.set(file("$rootDir/docs/kdoc"))
        }
    }
}

changelog {
    futureVersionTag = project.version.toString()
    customTagByIssueNumber = mapOf(6 to "0.1.1", 10 to "0.1.2", 15 to "0.4.0")
}

nexusPublishing {
    packageGroup.set("org.hildan")
    repositories {
        sonatype()
    }
    transitionCheckOptions {
        maxRetries.set(90) // sometimes Sonatype takes more than 10min...
    }
}

subprojects {

    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
        kotlinOptions.freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }

    tasks.withType<AbstractTestTask> {
        testLogging {
            events("failed", "standardOut", "standardError")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStackTraces = true
        }
    }
}
