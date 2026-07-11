plugins {
    kotlin("jvm")
}

kotlin {
    compilerOptions {
        progressiveMode = true
        freeCompilerArgs.add("-Xrender-internal-diagnostic-names")
    }
}
