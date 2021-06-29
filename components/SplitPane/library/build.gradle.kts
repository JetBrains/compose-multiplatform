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
            }
        }
        named("desktopMain") {}
    }
}

// TODO it seems that argument isn't applied to the common sourceSet. Figure out why
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

configureMavenPublication(
    groupId = "org.jetbrains.compose.components",
    artifactId = "components-splitpane",
    name = "SplitPane for Compose JB"
)