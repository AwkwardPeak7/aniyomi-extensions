plugins {
    id("com.android.library")
}

val ver = "14"
version = ver

android {
    compileSdk = 34
    namespace = "eu.kanade.tachiyomi.animeextensions"

    defaultConfig {
        minSdk = 21
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
        }
    }

    val javaVersion = JavaVersion.VERSION_1_8
    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

dependencies {
    compileOnly(libs.bundles.common)
}
