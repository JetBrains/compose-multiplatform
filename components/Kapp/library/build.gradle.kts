import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("maven-publish")
}

val withNative = true

kotlin {
    jvm("desktop")
    js(IR) {
        browser()
    }

    if (withNative) {
        iosX64("uikitX64")
        //iosArm64("uikitArm64")
        macosX64()
        // macosArm64()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                api(compose.ui)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
        val jsMain by getting

        if (withNative) {
            val darwinMain by creating {
                dependsOn(commonMain)
            }

            val macosMain by creating {
                dependsOn(darwinMain)
            }

            val macosX64Main by getting {
                dependsOn(macosMain)
            }

            /*
            val macosArm64Main by getting {
                dependsOn(macosMain)
            } */

            val uikitMain by creating {
                dependsOn(darwinMain)
            }

            val uikitX64Main by getting {
                dependsOn(uikitMain)
            }
            /*
            val uikitArm64Main by getting {
                dependsOn(uikitMain)
            } */
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

configureMavenPublication(
    groupId = "org.jetbrains.compose.components",
    artifactId = "components-app",
    name = "App API for Compose Multiplatform"
)
