plugins {
    id("com.android.library")
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.apollographql.apollo").version("2.3.0")
}

kotlin {
    androidTarget()
    jvm("desktop")

    sourceSets {
        named("commonMain") {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                implementation("org.jetbrains.compose.material:material-icons-core:1.6.11")
                implementation("com.apollographql.apollo:apollo-runtime:2.3.0")
                implementation("org.ocpsoft.prettytime:prettytime:4.0.4.Final")
            }
        }
        named("androidMain") {
            kotlin.srcDirs("src/jvmAndAndroidMain/kotlin")
            dependencies {
                api("androidx.activity:activity-compose:1.7.2")
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.10.1")
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
    compileSdk = 35
    namespace = "com.example.myapplication.common"

    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    sourceSets {
        named("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
        }
    }
}
