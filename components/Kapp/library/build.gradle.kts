import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("maven-publish")
}

kotlin {
    jvm("desktop")
    js(IR) {
        browser()
    }
    iosX64("uikitX64")
    iosArm64("uikitArm64")

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

        /*
        val uikitMain by creating {
            dependsOn(commonMain)
        }

        val uikitX64Main by getting {
            dependsOn(uikitMain)
        }
        val uikitArm64Main by getting {
            dependsOn(uikitMain)
        } */
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
