import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("maven-publish")
}

val osName: String = System.getProperty("os.name")
val os = when {
    osName == "Mac OS X" -> "macos"
    osName == "Linux" -> "linux"
    osName.startsWith("Win") -> "windows"
    else -> throw Error("Unknown OS $osName")
}

kotlin {
    jvm("desktop")

    sourceSets {
        named("desktopMain") {
            dependencies {
                implementation(compose.runtime)
                implementation("org.lwjgl:lwjgl:3.2.3")
                implementation("org.lwjgl:lwjgl-nfd:3.2.3")
                implementation("org.lwjgl:lwjgl:3.2.3:natives-$os") // TODO make a separate publication
                implementation("org.lwjgl:lwjgl-nfd:3.2.3:natives-$os") // TODO make a separate publication
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
    artifactId = "components-nativedialogs",
    name = "Native Dialogs for Compose JB"
)