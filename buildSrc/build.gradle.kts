plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())

    implementation(kotlin("gradle-plugin", "1.7.0"))
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.7.0") {
        // workaround while waiting for https://github.com/Kotlin/dokka/pull/2543
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }
}

gradlePlugin {
    plugins {
        register("kotlin-maven-central-publish") {
            id = "kotlin-maven-central-publish"
            implementationClass = "org.hildan.krossbow.gradle.plugins.KotlinMavenCentralPublishPlugin"
        }
    }
}
