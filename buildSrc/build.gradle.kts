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

    implementation(kotlin("gradle-plugin", "1.7.20"))
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.7.20")

    implementation("org.hildan.gradle:gradle-kotlin-publish-plugin:0.1.0")
    implementation("ru.vyarus:gradle-github-info-plugin:1.4.0")
}
