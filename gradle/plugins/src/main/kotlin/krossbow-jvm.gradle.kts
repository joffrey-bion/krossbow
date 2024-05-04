plugins {
    kotlin("jvm")
}

kotlin {
    compilerOptions {
        progressiveMode = true
        allWarningsAsErrors = true
        freeCompilerArgs.add("-Xjvm-default=all-compatibility")
    }
}
