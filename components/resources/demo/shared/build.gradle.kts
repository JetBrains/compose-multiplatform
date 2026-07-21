import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    android {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        androidResources.enable = true
    }
    jvm("desktop")
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }
    js {
        browser {
            testTask(Action {
                enabled = false
            })
        }
        binaries.executable()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    applyDefaultHierarchyTemplate()
    sourceSets {
        val desktopMain by getting
        val wasmJsMain by getting

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.core)
            implementation(project(":resources:library"))
        }
        desktopMain.dependencies {
            implementation(libs.compose.desktop)
        }
        androidMain.dependencies {
            implementation(libs.androidx.ui.tooling)
            implementation(libs.androidx.ui.tooling.preview)
        }

        val nonAndroidMain by creating {
            dependsOn(commonMain.get())
            wasmJsMain.dependsOn(this)
            desktopMain.dependsOn(this)
            nativeMain.get().dependsOn(this)
            jsMain.get().dependsOn(this)
        }
    }
    android {
        namespace = "org.jetbrains.compose.resources.demo.shared"
        compileSdk = 37
        minSdk = 23
    }
}

//because the dependency on the compose library is a project dependency
compose.resources {
    generateResClass = always
}
