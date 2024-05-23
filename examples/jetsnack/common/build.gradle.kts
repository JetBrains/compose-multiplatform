import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.library")
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    androidTarget()
    wasmJs { browser() }

    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "common"
            isStatic = true
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                implementation(libs.kotlinx.coroutines)
            }
        }
        val nonAndroidMain by creating {
            dependsOn(commonMain)
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.appcompat:appcompat:1.5.1")
                api("androidx.core:core-ktx:1.9.0")
                implementation(libs.coil.kt.compose)
                implementation(libs.androidx.navigation.compose)
                implementation(libs.androidx.compose.ui.util)
                implementation(libs.androidx.lifecycle.viewModelCompose)
                implementation(libs.androidx.constraintlayout.compose)
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.lifecycle.viewModelCompose)
                implementation(libs.androidx.lifecycle.runtime.compose)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }
        val desktopMain by getting {
            dependsOn(nonAndroidMain)
            dependencies {
                api(compose.preview)
            }
        }
        val desktopTest by getting

        val wasmJsMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
            }
            dependsOn(nonAndroidMain)
        }

        val iosMain by getting {
            dependsOn(nonAndroidMain)
        }
    }
}


android {
    compileSdk = 34
    namespace = "com.example.jetsnack"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 24
        targetSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}
