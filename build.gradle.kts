import org.hildan.github.changelog.builder.DEFAULT_EXCLUDED_LABELS
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    val kotlinVersion = "1.5.21"
    kotlin("jvm") version kotlinVersion apply false
    kotlin("js") version kotlinVersion apply false
    kotlin("multiplatform") version kotlinVersion apply false
    kotlin("plugin.spring") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false
    id("org.jetbrains.dokka") version "1.5.0" apply false
    id("org.hildan.github.changelog") version "1.8.0"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.7.1"
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
    id("com.avast.gradle.docker-compose") version "0.14.9"
}

// autobahn test server for websocket tests
dockerCompose {
    useComposeFiles.set(listOf(file("$rootDir/autobahn/docker-compose.yml").toString()))
    buildBeforeUp.set(false)
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
        tasks.withType<org.jetbrains.dokka.gradle.DokkaTask> {
            dokkaSourceSets.findByName("jsMain")?.suppress?.set(true)
            dokkaSourceSets.findByName("jsTest")?.suppress?.set(true)
        }

        // ensure autobahn test server is launched for websocket tests
        tasks.withType<AbstractTestTask> {
            rootProject.dockerCompose.isRequiredBy(this)
        }
        // provide autobahn test server coordinates to the tests (non-trivial on macOS)
        tasks.withType<KotlinJvmTest> {
            rootProject.dockerCompose.exposeAsEnvironment(this)
        }

        val generateAutobahnConfigJson = tasks.create("generateAutobahnConfigJson") {
            rootProject.dockerCompose.isRequiredBy(this)
            val config = "${rootProject.buildDir}/js/packages/${rootProject.name}-${project.name}-test/autobahn-server.json"
            outputs.file(config)
            doFirst {
                val autobahnContainer = rootProject.dockerCompose.servicesInfos["autobahn_server"]?.firstContainer
                    ?: error("autobahn_server container not found")
                file(config).writeText("""{"host":"${autobahnContainer.host}","port":${autobahnContainer.port}}""")
            }
        }

        extensions.findByType<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()?.js {
            nodejs {
                testTask {
                    dependsOn(generateAutobahnConfigJson)
                    // for krossbow-stomp-core tests and Autobahn tests
                    useMocha {
                        timeout = "10s"
                    }
                }
            }
            browser {
                testTask {
                    dependsOn(generateAutobahnConfigJson)
                    // for krossbow-stomp-core tests and Autobahn tests
                    useMocha {
                        timeout = "10s"
                    }
                }
            }
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
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

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

fun MavenPublication.configurePomForMavenCentral(project: Project) = pom {
    name.set(project.name)
    description.set(project.description)
    url.set(githubRepoUrl)
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
