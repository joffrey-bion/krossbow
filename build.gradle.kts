import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayExtension.*

plugins {
    val kotlinVersion = "1.3.40"
    kotlin("jvm") version kotlinVersion apply false
    kotlin("js") version kotlinVersion apply false
    kotlin("multiplatform") version kotlinVersion apply false
    kotlin("plugin.spring") version kotlinVersion apply false
    id("org.jlleitschuh.gradle.ktlint") version "7.1.0" apply false
    id("org.jetbrains.dokka") version "0.9.18" apply false
    id("com.jfrog.bintray") version "1.8.4" apply false
}

allprojects {
    group = "org.hildan.krossbow"
    version = "0.1.0"
}

val githubUser = "joffrey-bion"
val githubSlug = "$githubUser/${rootProject.name}"
val githubRepoUrl = "https://github.com/$githubSlug"
val Project.labels get() = arrayOf("websocket", "stomp", "krossbow", "multiplatform", "kotlin", "client")
val Project.licenses get() = arrayOf("MIT")

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "maven-publish")
    apply(plugin = "com.jfrog.bintray")

    repositories {
        jcenter()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    afterEvaluate {

        val publicationNames = extensions.getByType<PublishingExtension>().publications.names

        extensions.configure<BintrayExtension>("bintray") {
            user = getPropOrEnv("bintrayUser", "BINTRAY_USER")
            key = getPropOrEnv("bintrayApiKey", "BINTRAY_KEY")
            setPublications(*publicationNames.toTypedArray())
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
                        sync = true
                        user = getPropOrEnv("ossrhUserToken", "OSSRH_USER_TOKEN")
                        password = getPropOrEnv("ossrhKey", "OSSRH_KEY")
                    })
                })
            })
        }

        tasks["bintrayUpload"].dependsOn(tasks["build"])
    }
}

fun Project.getPropOrEnv(propName: String, envVar: String? = null): String? =
  findProperty(propName) as String? ?: System.getenv(envVar)

