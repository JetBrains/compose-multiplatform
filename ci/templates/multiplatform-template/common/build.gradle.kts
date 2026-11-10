import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.kotlin.multiplatform.library")
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    android {
        namespace = "com.myapplication.common"
        compileSdk = 37
        minSdk = 26
        androidResources.enable = true
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    jvm("desktop")

    sourceSets {
        named("commonMain") {
            dependencies {
                api(libs.compose.runtime)
                api(libs.compose.foundation)
                api(libs.compose.material)
                // Needed only for preview.
                implementation(libs.compose.ui.tooling.preview)
            }
        }
        named("androidMain") {
            dependencies {
                api("androidx.appcompat:appcompat:1.5.1")
                api("androidx.core:core-ktx:1.8.0")
            }
        }
    }
}

tasks.withType<KotlinCompile>() {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
