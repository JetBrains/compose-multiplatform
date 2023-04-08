plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

version = "1.0-SNAPSHOT"

kotlin {
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.materialIconsExtended)
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:2.2.1")
                implementation(compose.desktop.common)
            }
        }
    }
}
