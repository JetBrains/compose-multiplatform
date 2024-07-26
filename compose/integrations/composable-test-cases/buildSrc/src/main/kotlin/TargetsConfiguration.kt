import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

val Project.coroutinesVersion: String
    get() {
        return extraProperties.properties["kotlinx.coroutines.version"] as String
    }

@OptIn(ExperimentalWasmDsl::class)
fun KotlinMultiplatformExtension.configureTargets() {
    jvm("desktop")
    applyDefaultHierarchyTemplate()
    configureJsTargets()
    wasmJs {
        d8 {}
    }

    iosArm64()
    iosSimulatorArm64()
    iosX64()
    macosX64()
    macosArm64()
    // We use linux agents on CI. So it doesn't run the tests, but it builds the klib anyway which is time consuming.
    // if (project.isMingwX64Enabled) mingwX64()
    linuxX64()
}

fun KotlinMultiplatformExtension.configureJsTargets() {
    js(IR) {
        browser()
        // nodejs() // Commented to save a bit of CI time. Testing in a browser should be enough.
    }
}

fun KotlinDependencyHandler.getLibDependencyForMain(): ProjectDependency {
    if (!project.name.endsWith("-main")) error("Unexpected main module name: ${project.name}")
    return project(":" + project.name.replace("-main", "-lib"))
}

fun KotlinDependencyHandler.getCommonLib(): ProjectDependency {
    return project(":common")
}

fun KotlinSourceSet.configureCommonTestDependencies() {
    with(project) {
        dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
        }
    }
}

val Project.composeVersion: String
    get() = properties["compose.version"] as? String
        ?: error("'compose.version' is not defined")

val Project.composeRuntimeDependency: String
    get() = properties["compose.runtime.artifactId"] as? String
        ?: properties["compose.runtime.groupId"]?.let { "$it:runtime:$composeVersion" }
        ?: "org.jetbrains.compose.runtime:runtime:${composeVersion}"
