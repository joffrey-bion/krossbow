plugins {
    kotlin("jvm")
}

kotlin {
    sourceSets {
        all {
            languageSettings.optIn("org.hildan.krossbow.io.InternalKrossbowIoApi")
        }
    }
}
