plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlinx-serialization")
}

android {
    compileSdk = AndroidConfig.compileSdk

    defaultConfig {
        minSdk = AndroidConfig.minSdk
    }

    namespace = "io.github.awkwardpeak7.lib.${project.name}"

    buildFeatures {
        androidResources = false
    }
}

dependencies {
    compileOnly(project(":extension-lib"))
    compileOnly(versionCatalogs.named("libs").findBundle("common").get())
}
