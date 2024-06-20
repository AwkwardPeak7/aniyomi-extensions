plugins {
    id("com.android.library")
}

android {
    compileSdk = AndroidConfig.compileSdk
    namespace = "io.github.awkwardpeak7.extension.core"

    defaultConfig {
        minSdk = AndroidConfig.minSdk
    }

    sourceSets {
        named("main") {
            manifest.srcFile("AndroidManifest.xml")
            res.setSrcDirs(listOf("res"))
        }
    }
}
