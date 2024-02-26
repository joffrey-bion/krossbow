plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.java.websocket)
    implementation(libs.kotlinx.coroutines.core)
//    implementation(libs.ktor.server.netty)
//    implementation(libs.ktor.server.websockets)

//    runtimeOnly(libs.slf4j.simple)
}
