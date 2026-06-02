plugins {
    kotlin("jvm")
}

kotlin {
    compilerOptions {
        progressiveMode = true
        allWarningsAsErrors = true
        freeCompilerArgs.add("-Xrender-internal-diagnostic-names")
    }
}
