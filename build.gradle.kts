import org.hildan.github.changelog.builder.DEFAULT_EXCLUDED_LABELS

plugins {
    val kotlinVersion = "1.5.31"
    kotlin("jvm") apply false
    kotlin("js") apply false
    kotlin("multiplatform") apply false
    kotlin("plugin.spring") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false
    id("org.jetbrains.dokka") version "1.5.31" apply false
    id("org.hildan.github.changelog") version "1.8.0"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.7.1"
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
}

allprojects {
    group = "org.hildan.krossbow"

    repositories {
        mavenCentral()
    }

    apply(plugin = "org.jetbrains.dokka")

    afterEvaluate {
        // suppressing Dokka generation for JS because of the ZipException on NPM dependencies
        // https://github.com/Kotlin/dokka/issues/537
        tasks.withType<org.jetbrains.dokka.gradle.AbstractDokkaLeafTask> {
            dokkaSourceSets.findByName("jsMain")?.suppress?.set(true)
            dokkaSourceSets.findByName("jsTest")?.suppress?.set(true)
        }

        tasks.withType<org.jetbrains.dokka.gradle.DokkaMultiModuleTask> {
            outputDirectory.set(file("$rootDir/docs/kdoc"))
        }
    }
}

val Project.githubUser: String? get() = findProperty("githubUser") as String? ?: System.getenv("GITHUB_USER")
val githubSlug = "$githubUser/${rootProject.name}"
val githubRepoUrl = "https://github.com/$githubSlug"

changelog {
    githubUser = project.githubUser
    futureVersionTag = project.version.toString()
    excludeLabels = listOf("internal") + DEFAULT_EXCLUDED_LABELS
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
        kotlinOptions.freeCompilerArgs += listOf("-Xopt-in=kotlin.RequiresOptIn")
    }

    tasks.withType<AbstractTestTask> {
        testLogging {
            events("failed", "standardOut", "standardError")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStackTraces = true
        }
    }

    if (project.name != "autobahn-tests") {
        apply(plugin = "maven-publish")
        apply(plugin = "signing")

        val dokkaJar by tasks.creating(Jar::class) {
            archiveClassifier.set("javadoc")
            from(tasks.findByName("dokkaHtml"))
        }

        afterEvaluate {
            publishing.publications.filterIsInstance<MavenPublication>().forEach { pub ->
                pub.artifact(dokkaJar)
                pub.configurePomForMavenCentral(project)
            }

            signing {
                val signingKey: String? by project
                val signingPassword: String? by project
                useInMemoryPgpKeys(signingKey, signingPassword)
                sign(publishing.publications)
            }

            tasks["assemble"].dependsOn(tasks["dokkaHtml"])
        }
    }
}

fun MavenPublication.configurePomForMavenCentral(project: Project) = pom {
    name.set(project.name)
    description.set(project.description)
    url.set("https://joffrey-bion.github.io/krossbow")
    licenses {
        license {
            name.set("The MIT License")
            url.set("https://opensource.org/licenses/MIT")
        }
    }
    developers {
        developer {
            id.set("joffrey-bion")
            name.set("Joffrey Bion")
            email.set("joffrey.bion@gmail.com")
        }
    }
    scm {
        connection.set("scm:git:$githubRepoUrl.git")
        developerConnection.set("scm:git:git@github.com:$githubSlug.git")
        url.set(githubRepoUrl)
    }
}
