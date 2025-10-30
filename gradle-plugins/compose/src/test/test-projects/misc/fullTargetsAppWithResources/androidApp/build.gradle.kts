plugins {
    id("com.android.application")
}

android {
    namespace = "org.company.app.androidApp"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
        targetSdk = 36

        applicationId = "org.company.app.androidApp"
        versionCode = 1
        versionName = "1.0.0"
    }
    signingConfigs {
        create("testkey") {
            storeFile = project.file("key/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("testkey")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("testkey")
        }
    }
    flavorDimensions += "version"
    productFlavors {
        create("demo")
        create("full")
    }
    lint {
        checkReleaseBuilds = false
    }
}



dependencies {
    implementation(project(":sharedUI"))
}
