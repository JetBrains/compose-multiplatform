import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin

plugins {
    id("com.android.kotlin.multiplatform.library").apply(false)
    id("com.android.application").apply(false)
    id("org.jetbrains.kotlin.multiplatform").apply(false)
    id("org.jetbrains.kotlin.android").apply(false)
    id("org.jetbrains.kotlin.jvm").apply(false)
    id("org.jetbrains.kotlin.plugin.compose").apply(false)
    id("org.jetbrains.compose").apply(false)
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
