plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    maven(url = "https://plugins.gradle.org/m2/")
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())

    implementation(kotlin("gradle-plugin", libs.versions.kotlin.get()))
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:${libs.versions.dokka.get()}")
    implementation("org.jetbrains.kotlinx.binary-compatibility-validator:org.jetbrains.kotlinx.binary-compatibility-validator.gradle.plugin:${libs.versions.binary.compatibility.validator.plugin.get()}")

    implementation("org.hildan.gradle:gradle-kotlin-publish-plugin:${libs.versions.hildan.kotlin.publish.plugin.get()}")
    implementation("ru.vyarus:gradle-github-info-plugin:${libs.versions.vyarus.github.info.plugin.get()}")
}
