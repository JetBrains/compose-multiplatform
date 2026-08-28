@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test

plugins {
    kotlin("multiplatform")
}

val kotlinxBrowserVersion = providers.gradleProperty("kotlinx.browser.version").get()
val generatedSubset = project(":ksp-runner").layout.buildDirectory.dir(
    "generated/kotlinxBrowserCommonSubset",
)
val subsetDirectory = rootProject.layout.projectDirectory.dir("..")
val checkedInManifest = subsetDirectory.file("api/dom-api-manifest.txt")
val stagedInterop = layout.buildDirectory.dir("handwrittenInterop")
val stageInterop by tasks.registering(Sync::class) {
    from(subsetDirectory.dir("src")) {
        include("*/kotlin/kotlinx/browser/Interop.kt")
    }
    into(stagedInterop)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvm()
    js {
        nodejs()
        compilerOptions {
            optIn.add("kotlin.js.ExperimentalWasmJsInterop")
        }
    }
    wasmJs {
        nodejs()
        compilerOptions {
            optIn.add("kotlin.js.ExperimentalWasmJsInterop")
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(generatedSubset.map { it.dir("commonMain/kotlin") })
            kotlin.srcDir(stagedInterop.map { it.dir("commonMain/kotlin") })
        }
        val webMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-browser:$kotlinxBrowserVersion")
            }
        }
        val jsMain by getting {
            kotlin.srcDir(generatedSubset.map { it.dir("jsMain/kotlin") })
            kotlin.srcDir(stagedInterop.map { it.dir("jsMain/kotlin") })
        }
        val wasmJsMain by getting {
            kotlin.srcDir(generatedSubset.map { it.dir("wasmJsMain/kotlin") })
            kotlin.srcDir(stagedInterop.map { it.dir("wasmJsMain/kotlin") })
        }
        val jvmMain by getting {
            kotlin.srcDir(generatedSubset.map { it.dir("jvmMain/kotlin") })
            kotlin.srcDir(stagedInterop.map { it.dir("jvmMain/kotlin") })
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":"))
            }
        }
    }
}

tasks.matching { it.name.startsWith("compile") && "Kotlin" in it.name }.configureEach {
    dependsOn(":ksp-runner:generateKotlinxBrowserCommonSubset", stageInterop)
}

tasks.withType<Test>().configureEach {
    val model = generatedSubset.map { it.file("model.txt") }
    val coverage = generatedSubset.map { it.file("coverage.txt") }
    val apiManifest = generatedSubset.map { it.file("api-manifest.txt") }

    dependsOn(":ksp-runner:generateKotlinxBrowserCommonSubset")
    systemProperty("commonDomModel", model.get().asFile.absolutePath)
    systemProperty("commonDomCoverage", coverage.get().asFile.absolutePath)
    systemProperty("commonDomApiManifest", apiManifest.get().asFile.absolutePath)
    systemProperty("commonDomApiManifestBaseline", checkedInManifest.asFile.absolutePath)
    inputs.files(model, coverage, apiManifest, checkedInManifest)
}
