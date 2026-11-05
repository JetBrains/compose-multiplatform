import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("co.touchlab.kmmbridge").version("0.5.7")
}

kotlin {
    js {
        browser { }
        binaries.executable()
    }

    wasmJs {
        browser { }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:COMPOSE_VERSION_PLACEHOLDER")
        }

        val webMain by creating { dependsOn(commonMain.get()) }
        jsMain { dependsOn(webMain) }
        wasmJsMain { dependsOn(webMain) }
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
