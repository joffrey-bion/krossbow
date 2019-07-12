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
    id("org.hildan.github.changelog") version "0.8.0"
}

allprojects {
    group = "org.hildan.krossbow"
    version = "0.3.1"
}

val githubUser = "joffrey-bion"
val githubSlug = "$githubUser/${rootProject.name}"
val githubRepoUrl = "https://github.com/$githubSlug"
val Project.labels get() = arrayOf("websocket", "stomp", "krossbow", "multiplatform", "kotlin", "client")
val Project.licenses get() = arrayOf("MIT")

changelog {
    futureVersionTag = project.version.toString()
    excludeLabels = listOf("internal")
    customTagByIssueNumber = mapOf(6 to "0.1.1", 10 to "0.1.2")
}

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

        val publications = extensions.getByType<PublishingExtension>().publications
        publications.mapNotNull { it as? MavenPublication }.forEach {
            it.configurePomForMavenCentral(project)
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
