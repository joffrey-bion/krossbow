plugins {
    kotlin("jvm")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all-compatibility")
    }
}
