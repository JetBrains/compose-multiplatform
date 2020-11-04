import org.jetbrains.compose.compose

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.apollographql.apollo").version("2.3.0")
}

kotlin {
    android()
    jvm("desktop")

    sourceSets {
        named("commonMain") {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                implementation("com.apollographql.apollo:apollo-runtime:2.3.0")
                implementation("org.ocpsoft.prettytime:prettytime:4.0.4.Final")
            }
        }
        named("androidMain") {
            kotlin.srcDirs("src/jvmAndAndroidMain/kotlin")
            dependencies {
                api("androidx.appcompat:appcompat:1.1.0")
                api("androidx.core:core-ktx:1.3.1")
            }
        }
        named("desktopMain") {
            kotlin.srcDirs("src/jvmAndAndroidMain/kotlin")
        }
    }
}

apollo {
    generateKotlinModels.set(true)
    customTypeMapping.putAll(mapOf("DateTime" to "java.util.Date"))
}

android {
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(26)
        targetSdkVersion(30)
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    sourceSets {
        named("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")


//            res.srcDirs("src/androidMain/res")
        }
    }
}
