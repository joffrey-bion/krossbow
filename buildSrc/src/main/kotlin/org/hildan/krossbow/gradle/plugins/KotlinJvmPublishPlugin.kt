package org.hildan.krossbow.gradle.plugins

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

/**
 * Adds sourcesJar task and registers publications for pure Kotlin JVM projects (not MPP).
 */
class KotlinJvmPublishPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // KotlinPluginWrapper is the Kotlin JVM plugin
        val hasKotlinJvmPlugin = project.plugins.withType<KotlinPluginWrapper>().isNotEmpty()
        if (!hasKotlinJvmPlugin) {
            throw GradleException("This plugin only applies to projects with Kotlin JVM plugin, not Multiplatform")
        }

        // sourcesJar is not added by default by the Kotlin JVM plugin
        project.extensions.getByType<JavaPluginExtension>().apply {
            withSourcesJar()
        }

        project.apply<MavenPublishPlugin>()
        project.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    from(project.components["java"]) // java components have the sourcesJar AND the Kotlin artifacts
                }
            }
        }
    }
}
