import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("maven-publish")
}

kotlin {
    jvm("desktop")

    sourceSets {
        named("commonMain") {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                api(compose.materialIconsExtended)
            }
        }
        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.common)
                implementation("org.jetbrains.compose.components:components-codeeditor-platform-api:${version}")
                implementation("org.jetbrains.compose.components:components-codeeditor-platform-lib:${version}")
            }
        }
    }
}

// TODO it seems that argument isn't applied to the common sourceSet. Figure out why
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

configureMavenPublication(
    groupId = "org.jetbrains.compose.components",
    artifactId = "components-codeeditor",
    name = "CodeEditor for Compose JB"
)
