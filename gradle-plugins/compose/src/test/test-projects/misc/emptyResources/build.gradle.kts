plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "app.group"

kotlin {
    jvm("desktop")

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.material)
                implementation(compose.components.resources)
            }
        }
    }
}
