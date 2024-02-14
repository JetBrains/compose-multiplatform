plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("maven-publish")
}

kotlin {
    jvm("desktop")
    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
            }
        }
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.foundation)
        }
    }
}

configureMavenPublication(
    groupId = "org.jetbrains.compose.components",
    artifactId = "components-animatedimage",
    name = "AnimatedImage for Compose JB"
)