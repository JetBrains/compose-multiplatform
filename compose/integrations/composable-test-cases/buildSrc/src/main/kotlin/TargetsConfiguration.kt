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

val Project.isInIdea: Boolean
    get() {
        return System.getProperty("idea.active")?.toBoolean() == true
    }

val Project.isFailingJsCase: Boolean
    get() = this.name.contains("-failingJs-")

val Project.isMingwX64Enabled: Boolean
    get() = false //this.isInIdea

@OptIn(ExperimentalWasmDsl::class)
fun KotlinMultiplatformExtension.configureTargets() {
    jvm("desktop")
    configureJsTargets()
    wasmJs {
        d8 {}
    }
    ios()
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