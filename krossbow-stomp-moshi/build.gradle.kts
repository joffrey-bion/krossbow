plugins {
    kotlin("jvm")
    id("krossbow.kotlin-publishing-conventions")
}

description = "An extension of Krossbow STOMP client using Moshi for message conversions"

dependencies {
    api(projects.krossbowStompCore)
    api(libs.moshi)
    testImplementation(kotlin("test"))
    testImplementation(libs.moshiKotlin) // for reflection-based serialization of Kotlin classes
}

dokkaExternalDocLink("https://square.github.io/moshi/1.x/moshi/")
