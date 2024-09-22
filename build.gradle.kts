plugins {
    alias(libs.plugins.dokka)
    alias(libs.plugins.hildan.github.changelog)
    alias(libs.plugins.nexus.publish)
    // Commented out for now because of https://github.com/LouisCAD/CompleteKotlin/issues/15
//    alias(libs.plugins.louiscad.complete.kotlin) // for autocomplete of Apple libraries on non-macOS systems
    id("krossbow-githubinfo")

    // The following plugins are only added as a Workaround for https://github.com/gradle/gradle/issues/17559.
    // In short, we need to align the plugin classpath between all projects in the build.
    alias(libs.plugins.kotlin.atomicfu) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    group = "org.hildan.krossbow"
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaMultiModuleTask>().configureEach {
    outputDirectory.set(file("$rootDir/docs/kdoc"))
}

changelog {
    githubUser = github.user
    futureVersionTag = project.version.toString()
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
