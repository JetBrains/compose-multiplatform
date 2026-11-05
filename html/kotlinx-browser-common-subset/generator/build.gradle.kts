import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin

plugins {
    kotlin("jvm")
}

group = "org.jetbrains.compose.html.build"
version = "1.0"

dependencies {
    implementation(
        "com.google.devtools.ksp:symbol-processing-api:${providers.gradleProperty("ksp.version").get()}",
    )
    implementation("com.squareup:kotlinpoet-jvm:2.2.0")
    testImplementation(kotlin("test"))
}

val generatedCommonSource = project(":ksp-runner").layout.buildDirectory.file(
    "generated/kotlinxBrowserCommonSubset/commonMain/kotlin/kotlinx/browser/dom/Dom.kt",
)
val generatedCommonMetadata = project(":verification").layout.buildDirectory.file(
    "classes/kotlin/metadata/commonMain/default/manifest",
)

tasks.test {
    dependsOn(":verification:compileCommonMainKotlinMetadata")
    inputs.file(generatedCommonSource).withPropertyName("generatedCommonDomSource")
    inputs.file(generatedCommonMetadata).withPropertyName("generatedCommonDomMetadata")
    systemProperty("commonDomSource", generatedCommonSource.get().asFile.absolutePath)
    systemProperty("commonDomMetadata", generatedCommonMetadata.get().asFile.absolutePath)
}

tasks.check {
    dependsOn(":verification:check")
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
