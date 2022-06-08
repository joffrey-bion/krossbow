plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())

    val kotlinVersion = "1.6.21"
    implementation(kotlin("gradle-plugin", kotlinVersion))
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$kotlinVersion")
}

gradlePlugin {
    plugins {
        register("kotlin-maven-central-publish") {
            id = "kotlin-maven-central-publish"
            implementationClass = "org.hildan.krossbow.gradle.plugins.KotlinMavenCentralPublishPlugin"
        }
    }
}
