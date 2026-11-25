import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin

plugins {
    id("org.jetbrains.compose").apply(false)
    kotlin("multiplatform").apply(false)
    kotlin("plugin.compose").apply(false)
    id("com.android.kotlin.multiplatform.library").apply(false)
}

// TODO https://youtrack.jetbrains.com/issue/CMP-11110 Remove compromised root certificate workaround after updating to Kotlin 2.5.0
// Override the node version to fix https://youtrack.jetbrains.com/issue/CMP-11011
// Workaround is from https://youtrack.jetbrains.com/issue/KT-90732
// Subprojects apply the same workaround themselves: `allprojects` is not compatible with Gradle isolated projects
plugins.withType<NodeJsPlugin>().configureEach {
    extensions.getByType(NodeJsEnvSpec::class.java).version.set("26.12.1")
}
plugins.withType<WasmNodeJsPlugin>().configureEach {
    extensions.getByType(WasmNodeJsEnvSpec::class.java).version.set("26.12.1")
}
