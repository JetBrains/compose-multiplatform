import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

repositories {
    google()
    mavenCentral {
        url = uri("https://cache-redirector.jetbrains.com/maven-central")
    }
    maven("https://packages.jetbrains.team/maven/p/cmp/dev")
    mavenLocal()
}

kotlin {
    js(IR) {
        nodejs {}
        browser() {}
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(compose.runtime)
                implementation(project(":lib"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
            }
        }
    }
}

tasks.findByName("jsBrowserProductionWebpack")!!.mustRunAfter("jsDevelopmentExecutableCompileSync")
tasks.findByName("jsNodeDevelopmentRun")!!.mustRunAfter("jsProductionExecutableCompileSync")

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
