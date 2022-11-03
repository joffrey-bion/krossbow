plugins {
    kotlin("jvm")
    id("krossbow-publish")
}

description = "An extension of Krossbow STOMP client using Jackson for message conversions"

dependencies {
    api(projects.krossbowStompCore)
    api(platform(libs.jackson.bom))
    api(libs.jackson.core)
    api(libs.jackson.module.kotlin)

    testImplementation(kotlin("test"))
}

val jacksonMinorVersion = libs.versions.jackson.get().split(".").take(2).joinToString(".")
dokkaExternalDocLink("https://fasterxml.github.io/jackson-databind/javadoc/$jacksonMinorVersion/")
