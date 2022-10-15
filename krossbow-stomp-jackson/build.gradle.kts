plugins {
    kotlin("jvm")
    id("krossbow.kotlin-publishing-conventions")
}

description = "An extension of Krossbow STOMP client using Jackson for message conversions"

dependencies {
    api(projects.krossbowStompCore)
    api(libs.jackson.core)
    api(libs.jackson.module.kotlin)

    testImplementation(kotlin("test"))
}

val jacksonMinorVersion = libs.versions.jackson.get().substringBeforeLast(".")
dokkaExternalDocLink("https://fasterxml.github.io/jackson-databind/javadoc/$jacksonMinorVersion/")
