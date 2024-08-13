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
    artifactId = "components-splitpane",
    name = "SplitPane for Compose JB"
)