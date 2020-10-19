import org.jetbrains.compose.compose

plugins {
    id("kotlin-multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    sourceSets {
        named("commonMain") {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
            }
        }
        named("androidMain") {
            dependencies {
                api("androidx.appcompat:appcompat:1.1.0")
                api("androidx.core:core-ktx:1.3.1")
            }
        }
        named("desktopMain") {
            dependencies {
                api(compose.desktop.common)
            }
        }
    }
}
