@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.compose.gradle.standardConf
import org.jetbrains.compose.gradle.standardWasmConf

val kotlinxBrowserCommonSubsetVersion: String =
    providers.gradleProperty("compose.html.eap.kotlinx-browser-common-subset.version").get()
val composeHtmlEapVersion: String =
    providers.gradleProperty("compose.html.eap.version").get()

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}


repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        browser() {
            testTask {
                useKarma {
                    standardConf()
                }
            }
        }
    }
    wasmJs {
        browser() {
            testTask {
                useKarma {
                    standardWasmConf()
                }
            }
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(kotlin("stdlib-common"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val webMain by getting {
            dependencies {
                api(
                    "org.jetbrains.compose.html:" +
                        "kotlinx-browser-common-subset:$kotlinxBrowserCommonSubsetVersion"
                )
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(project(":internal-html-core-runtime"))
            }
        }
        val wasmJsMain by getting {
            dependencies {
                val localEapRuntime = project.findProject(":internal-html-core-runtime-eap")
                implementation(
                    localEapRuntime
                        ?: "org.jetbrains.compose.html.eap:" +
                            "internal-html-core-runtime-eap:$composeHtmlEapVersion"
                )
            }
        }
        val webTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
