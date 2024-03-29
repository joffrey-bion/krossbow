plugins {
    id("krossbow-multiplatform")
    id("krossbow-publish")
}

description = "Internal IO utilities for kotlinx-io conversions"

kotlin {
    allTargets()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlinx.io.bytestring)
            }
        }
    }
}

apiValidation {
    nonPublicMarkers += listOf("org.hildan.krossbow.io.InternalKrossbowIoApi")
}
