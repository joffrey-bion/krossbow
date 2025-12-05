plugins {
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.dokka")
    id("krossbow-githubinfo")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    signing
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set(project.name)
        description.set(project.description)
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
        }
    }
}

// ensures slash separator even on Windows, useful for URLs creation
private fun File.toSlashSeparatedString(): String = toPath().joinToString("/")
