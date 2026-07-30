plugins {
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.dokka")
    id("krossbow-githubinfo")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    signing
}

group = "org.hildan.krossbow"

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    pom {
        // Read lazily because the description is set in the module's build script *after* this plugin is applied,
        // so it would be null if read eagerly here. The project.name could also be overridden in the build script.
        name.set(project.provider { project.name })
        description.set(project.provider { project.description })
        developers {
            developer {
                id.set("joffrey-bion")
                name.set("Joffrey Bion")
                email.set("joffrey.bion@gmail.com")
            }
        }
    }
}

dokka {
    dokkaSourceSets {
        configureEach {
            sourceRoots.forEach { sourceRootDir ->
                val sourceRootRelativePath = sourceRootDir.relativeTo(rootProject.projectDir).toSlashSeparatedString()
                sourceLink {
                    localDirectory.set(sourceRootDir)
                    // HEAD points to the default branch of the repo.
                    remoteUrl("${github.repositoryUrl}/blob/HEAD/$sourceRootRelativePath")
                }
            }
            externalDocumentationLinks.register("kotlinx.serialization") {
                url("https://kotlin.github.io/kotlinx.serialization/")
            }
            externalDocumentationLinks.register("kotlinx.coroutines") {
                url("https://kotlinlang.org/api/kotlinx.coroutines/")
            }
        }
    }
}

// ensures slash separator even on Windows, useful for URLs creation
private fun File.toSlashSeparatedString(): String = toPath().joinToString("/")
