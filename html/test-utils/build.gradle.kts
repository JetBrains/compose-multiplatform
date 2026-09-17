@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.compose.gradle.standardConf

val kotlinxBrowserCommonSubsetVersion: String =
    providers.gradleProperty("compose.html.eap.kotlinx-browser-common-subset.version").get()
val composeHtmlEapEnabled = project.findProject(":internal-html-core-runtime-eap") != null

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
        browser {
            testTask {
                useKarma {
                    standardConf()
                }
            }
        }
    }
    if (composeHtmlEapEnabled) {
        wasmJs {
            browser {
                testTask {
                    useKarma {
                        standardConf()
                    }
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
                implementation(
                    "org.jetbrains.compose.html:" +
                        "kotlinx-browser-common-subset:$kotlinxBrowserCommonSubsetVersion"
                )
            }
        }
        val jsMain by getting {
            dependencies {
                // Stable Compose HTML tests must keep using the stable runtime.
                implementation(project(":internal-html-core-runtime"))
            }
        }
        if (composeHtmlEapEnabled) {
            val wasmJsMain by getting {
                dependencies {
                    // The Wasm target exists only for EAP tests and uses their browser runtime.
                    implementation(project(":internal-html-core-runtime-eap"))
                }
            }
        }
        val webTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
