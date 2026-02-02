import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

repositories {
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/cmp/dev")
    google()
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(npm("highlight.js", "10.7.2"))
                implementation(compose.html.core)
                implementation(compose.runtime)
            }
        }
    }
}
