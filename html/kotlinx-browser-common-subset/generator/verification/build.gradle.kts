@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    kotlin("multiplatform")
}

val kotlinxBrowserVersion = providers.gradleProperty("kotlinx.browser.version").get()
val generatedSubset = project(":runner").layout.buildDirectory.dir(
    "generated/kotlinxBrowserCommonSubset",
)

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
        }
        val webMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-browser:$kotlinxBrowserVersion")
            }
        }
        val jsMain by getting {
            kotlin.srcDir(generatedSubset.map { it.dir("jsMain/kotlin") })
        }
        val wasmJsMain by getting {
            kotlin.srcDir(generatedSubset.map { it.dir("wasmJsMain/kotlin") })
        }
        val jvmMain by getting {
            kotlin.srcDir(generatedSubset.map { it.dir("jvmMain/kotlin") })
        }
    }
}

tasks.matching { it.name.startsWith("compile") && "Kotlin" in it.name }.configureEach {
    dependsOn(":runner:generateKotlinxBrowserCommonSubset")
}
