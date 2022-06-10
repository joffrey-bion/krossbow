package org.hildan.krossbow.gradle.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.hildan.krossbow.gradle.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.dokka.gradle.*
import java.io.File
import java.net.URL

/**
 * Adds Dokka documentation as javadoc jar to the all publications, and sign them.
 *
 * Also reacts to the Kotlin/JVM plugin to add the relevant publications with sources jar so it's on par with MPP.
 *
 * The plugin relies on the following project properties to be present:
 * - `githubUser` - to generate proper git-scm URLs in Maven Central POMs
 * - `signingKey` - the ASCII armored GPG key to use
 * - `signingPassword` - the password for the GPG key to use
 *
 * The plugin makes the following assumptions for Dokka source links:
 * - the main branch of the GitHub repository is named "main"
 */
class KotlinMavenCentralPublishPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // KotlinPluginWrapper is the Kotlin JVM plugin
        val hasKotlinJvmPlugin = project.plugins.withType<KotlinPluginWrapper>().isNotEmpty()

        // Publications are not automatically configured with Kotlin JVM plugin, but they are with MPP.
        // We align here so both MPP and JVM projects have publications with sources jar setup.
        if (hasKotlinJvmPlugin) {
            project.apply<KotlinJvmPublishPlugin>()
        }

        project.apply<DokkaPlugin>()
        project.apply<MavenPublishPlugin>()
        project.apply<SigningPlugin>()

        project.configureDokkaSourceLinks()

        val dokkaJar by project.tasks.registering(Jar::class) {
            archiveClassifier.set("javadoc")
            from(project.tasks.named("dokkaHtml"))
        }

        // we wait for publications to be actually registered before modifying/signing
        project.afterEvaluate {

            project.configure<PublishingExtension> {
                publications.filterIsInstance<MavenPublication>().forEach { pub ->
                    pub.artifact(dokkaJar)
                    pub.configurePomForMavenCentral(project)
                }
            }

            project.signAllPublications()
        }
    }
}

private fun Project.configureDokkaSourceLinks(repoMainBranch: String = "main") {
    val repoBlobBaseUrl = URL("${github.repoUrl}/blob/$repoMainBranch/") // trailing slash is important in URLs!

    tasks.withType<AbstractDokkaLeafTask>().configureEach {
        dokkaSourceSets {
            configureEach {
                sourceRoots.forEach { sourceRootDir ->
                    val sourceRootRelativePath = sourceRootDir.relativeTo(rootProject.projectDir).toSlashSeparatedString()
                    sourceLink {
                        localDirectory.set(sourceRootDir)
                        remoteUrl.set(URL(repoBlobBaseUrl, sourceRootRelativePath))
                    }
                }
            }
        }
    }
}

// ensures slash separator even on Windows, useful for URLs creation
private fun File.toSlashSeparatedString(): String = toPath().joinToString("/")

private fun MavenPublication.configurePomForMavenCentral(project: Project) = pom {
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
    githubScm(project.github)
}

private fun MavenPom.githubScm(github: GitHubInfo) = scm {
    connection.set("scm:git:${github.repoUrl}.git")
    developerConnection.set("scm:git:git@github.com:${github.repoSlug}.git")
    url.set(github.repoUrl)
}

private fun Project.signAllPublications() {
    val project = this
    configure<SigningExtension> {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(extensions.getByType<PublishingExtension>().publications)
    }
}
