import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayExtension.*
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import org.gradle.api.publish.maven.internal.artifact.FileBasedMavenArtifact
import org.hildan.github.changelog.builder.DEFAULT_EXCLUDED_LABELS

plugins {
    val kotlinVersion = "1.4.31"
    kotlin("jvm") version kotlinVersion apply false
    kotlin("js") version kotlinVersion apply false
    kotlin("multiplatform") version kotlinVersion apply false
    kotlin("plugin.spring") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false
    id("org.jetbrains.dokka") version "1.4.30" apply false
    id("com.jfrog.bintray") version "1.8.5" apply false
    id("org.hildan.github.changelog") version "1.6.0"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.2.3"
}

allprojects {
    group = "org.hildan.krossbow"

    repositories {
        jcenter()
    }

    apply(plugin = "org.jetbrains.dokka")

    afterEvaluate {
        // suppressing Dokka generation for JS because of the annoying ZipException on NPM dependencies
        // https://github.com/Kotlin/dokka/issues/537
        tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
            dokkaSourceSets.findByName("jsMain")?.suppress?.set(true)
            dokkaSourceSets.findByName("jsTest")?.suppress?.set(true)
        }
    }
}

val Project.githubUser get() = getPropOrEnv("githubUser", "GITHUB_USER")
val githubSlug = "$githubUser/${rootProject.name}"
val githubRepoUrl = "https://github.com/$githubSlug"
val Project.labels get() = arrayOf("websocket", "stomp", "krossbow", "multiplatform", "kotlin", "client")
val Project.licenses get() = arrayOf("MIT")

changelog {
    githubUser = project.githubUser
    futureVersionTag = project.version.toString()
    excludeLabels = listOf("internal") + DEFAULT_EXCLUDED_LABELS
    customTagByIssueNumber = mapOf(6 to "0.1.1", 10 to "0.1.2", 15 to "0.4.0")
}

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "com.jfrog.bintray")

    val compilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
        kotlinOptions.freeCompilerArgs += compilerArgs
        //kotlinOptions.allWarningsAsErrors = true
    }

    tasks.withType<AbstractTestTask> {
        testLogging {
            events("failed", "standardOut", "standardError")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStackTraces = true
        }
    }

    afterEvaluate {
        val publications = extensions.getByType<PublishingExtension>().publications
        publications.filterIsInstance<MavenPublication>().forEach { pub ->
            pub.configurePomForMavenCentral(project)
        }

        extensions.configure<BintrayExtension>("bintray") {
            user = getPropOrEnv("bintrayUser", "BINTRAY_USER")
            key = getPropOrEnv("bintrayApiKey", "BINTRAY_KEY")
            setPublications(*publications.names.toTypedArray())
            publish = true

            pkg(closureOf<PackageConfig> {
                repo = getPropOrEnv("bintrayRepo", "BINTRAY_REPO")
                name = project.name
                desc = project.description
                setLabels(*project.labels)
                setLicenses(*project.licenses)

                websiteUrl = githubRepoUrl
                issueTrackerUrl = "$githubRepoUrl/issues"
                vcsUrl = "$githubRepoUrl.git"
                githubRepo = githubSlug

                version(closureOf<VersionConfig> {
                    desc = project.description
                    vcsTag = project.version.toString()
                    gpg(closureOf<GpgConfig> {
                        sign = true
                    })
                    mavenCentralSync(closureOf<MavenCentralSyncConfig> {
                        sync = false // TODO re-activate when javadoc stuff is fixed
                        user = getPropOrEnv("ossrhUserToken", "OSSRH_USER_TOKEN")
                        password = getPropOrEnv("ossrhKey", "OSSRH_KEY")
                    })
                })
            })
        }

        tasks["assemble"].dependsOn(tasks["dokkaHtml"])
        tasks["bintrayUpload"].dependsOn(tasks["build"])

        // Workaround bintray plugin issue for Gradle metadata publishing
        // https://github.com/bintray/gradle-bintray-plugin/issues/229
        tasks.withType<BintrayUploadTask> {
            doFirst {
                publications
                    .filterIsInstance<MavenPublication>()
                    .forEach { pub ->
                        val moduleFile = buildDir.resolve("publications/${pub.name}/module.json")
                        if (moduleFile.exists()) {
                            pub.artifact(object : FileBasedMavenArtifact(moduleFile) {
                                override fun getDefaultExtension() = "module"
                            })
                        }
                    }
            }
        }
    }
}

fun Project.getPropOrEnv(propName: String, envVar: String? = null): String? =
  findProperty(propName) as String? ?: System.getenv(envVar)

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
