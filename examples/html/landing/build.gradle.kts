import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin

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

allprojects {
    val subproject = this
    // Override the node version to fix https://youtrack.jetbrains.com/issue/CMP-11011
    // Workaround is from https://youtrack.jetbrains.com/issue/KT-90732
    subproject.plugins.withType<NodeJsPlugin>().configureEach {
        subproject.extensions.getByType(NodeJsEnvSpec::class.java).version.set(libs.versions.node)
    }
    subproject.plugins.withType<WasmNodeJsPlugin>().configureEach {
        subproject.extensions.getByType(WasmNodeJsEnvSpec::class.java).version.set(libs.versions.node)
    }
}
