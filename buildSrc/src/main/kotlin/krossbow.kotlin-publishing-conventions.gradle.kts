plugins {
    id("org.jetbrains.dokka")
    id("krossbow.githubinfo-conventions")
    id("kotlin-publish")
    signing
}

configure<PublishingExtension> {
    // configureEach reacts on new publications being registered and configures them too
    publications.configureEach {
        if (this is MavenPublication) {
            pom {
                developers {
                    developer {
                        id.set("joffrey-bion")
                        name.set("Joffrey Bion")
                        email.set("joffrey.bion@gmail.com")
                    }
                }
            }
        }
    }
}

configure<SigningExtension> {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(extensions.getByType<PublishingExtension>().publications)
}
