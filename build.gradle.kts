plugins {
    val kotlinVersion = "1.3.31"
    kotlin("jvm") version kotlinVersion apply false
    kotlin("multiplatform") version kotlinVersion apply false
    kotlin("plugin.spring") version kotlinVersion apply false
    id("kotlin2js") version kotlinVersion apply false
    id("org.jetbrains.kotlin.frontend") version "0.0.45" apply false
    id("org.jlleitschuh.gradle.ktlint") version "7.1.0" apply false
}

allprojects {
    group = "org.hildan.krossbow"
    version = "0.1.0"
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "maven-publish")

    repositories {
        jcenter()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}
