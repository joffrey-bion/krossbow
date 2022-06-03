plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation(kotlin("gradle-plugin", "1.6.21"))
}

gradlePlugin {
    plugins {
        register("kotlin-maven-central-publish") {
            id = "kotlin-maven-central-publish"
            implementationClass = "org.hildan.krossbow.gradle.plugins.KotlinMavenCentralPublishPlugin"
        }
    }
}
