import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

repositories {
    mavenCentral {
        url = uri("https://cache-redirector.jetbrains.com/maven-central")
    }
    maven("https://packages.jetbrains.team/maven/p/cmp/dev")
    google()
}

kotlin {
    js {
        browser()
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            kotlin.srcDir("src/main/kotlin")
            resources.srcDir("src/main/resources")

            dependencies {
                implementation(libs.compose.html.core)
                implementation(libs.compose.runtime)
            }
        }
    }
}

allprojects {
    val subproject = this
    subproject.plugins.withType<NodeJsPlugin>().configureEach {
        subproject.extensions.getByType(NodeJsEnvSpec::class.java).version.set(libs.versions.node)
    }
    subproject.plugins.withType<WasmNodeJsPlugin>().configureEach {
        subproject.extensions.getByType(WasmNodeJsEnvSpec::class.java).version.set(libs.versions.node)
    }
}
