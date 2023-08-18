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
    testImplementation(libs.kotlinx.coroutines.test)
}

dokkaExternalDocLink("https://www.javadoc.io/doc/com.fasterxml.jackson.core/jackson-databind/${libs.versions.jackson.get()}/")
