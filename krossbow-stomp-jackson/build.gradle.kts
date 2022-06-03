plugins {
    kotlin("jvm")
    `kotlin-maven-central-publish`
}

description = "An extension of Krossbow STOMP client using Jackson for message conversions"

dependencies {
    api(projects.krossbowStompCore)
    api(libs.jackson.core)
    api(libs.jackson.module.kotlin)

    testImplementation(kotlin("test"))
}
