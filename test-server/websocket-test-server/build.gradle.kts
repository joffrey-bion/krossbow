plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.java.websocket)
    implementation(libs.kotlinx.coroutines.core)
}
