@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.gradle.api.tasks.testing.Test
import org.jetbrains.compose.gradle.standardConf

plugins {
    kotlin("multiplatform")
}

val browserIdentityTestSources = layout.projectDirectory.dir("src/browserIdentityTest/kotlin")

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvm()
    js(IR) {
        browser {
            testTask {
                useKarma {
                    standardConf()
                }
            }
        }
        compilerOptions {
            optIn.add("kotlin.js.ExperimentalWasmJsInterop")
        }
    }
    wasmJs {
        browser {
            testTask {
                useKarma {
                    standardConf()
                }
            }
        }
        compilerOptions {
            optIn.add("kotlin.js.ExperimentalWasmJsInterop")
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val webMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jsTest by getting {
            kotlin.srcDir(browserIdentityTestSources)
        }
        val wasmJsTest by getting {
            kotlin.srcDir(browserIdentityTestSources)
        }
        val jvmTest by getting {
            dependencies {
                implementation("org.jetbrains.compose.html.build:kotlinx-browser-common-subset-generator:1.0")
            }
        }
    }
}

val generatorBuild = gradle.includedBuild("kotlinx-browser-common-subset-generator")
val generateSubset = generatorBuild.task(":runner:generateKotlinxBrowserCommonSubset")
val checkSubset = generatorBuild.task(":runner:checkKotlinxBrowserCommonSubset")
val checkGenerator = generatorBuild.task(":check")
val generatedReports = layout.projectDirectory.dir(
    "generator/runner/build/generated/kotlinxBrowserCommonSubset",
)
val checkedInManifest = layout.projectDirectory.file("api/dom-api-manifest.txt")

tasks.withType<Test>().configureEach {
    dependsOn(generateSubset)
    systemProperty("portableDomModel", generatedReports.file("model.txt").asFile.absolutePath)
    systemProperty("portableDomCoverage", generatedReports.file("coverage.txt").asFile.absolutePath)
    systemProperty("portableDomApiManifest", generatedReports.file("api-manifest.txt").asFile.absolutePath)
    systemProperty("portableDomApiManifestBaseline", checkedInManifest.asFile.absolutePath)
    inputs.files(
        generatedReports.file("model.txt"),
        generatedReports.file("coverage.txt"),
        generatedReports.file("api-manifest.txt"),
        checkedInManifest,
    )
}

tasks.named("check") {
    dependsOn(checkSubset, checkGenerator)
}
