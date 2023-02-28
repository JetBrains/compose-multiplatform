import org.jetbrains.compose.gradle.standardConf

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}


kotlin {
    jvm()
    js(IR) {
        browser() {
            testTask {
                useKarma {
                    standardConf()
                }
            }
        }
        binaries.executable()
    }
    wasm { browser() }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
            }
        }

        val jsWasmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(project(":internal-web-core-runtime"))
            }
        }

        val jsMain by getting {
            dependsOn(jsWasmMain)
            languageSettings {
                optIn("org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi")
            }
        }

        val wasmMain by getting {
            dependsOn(jsWasmMain)
            languageSettings {
                optIn("org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi")
            }
        }

        val jsWasmTest by creating {
            dependencies {
                implementation(project(":test-utils"))
                implementation(kotlin("test"))
            }
        }

        val jsTest by getting {
            dependsOn(jsWasmTest)
            languageSettings {
                optIn("org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi")
                optIn("org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi")
            }
            dependencies {
                implementation(project(":test-utils"))
                implementation(kotlin("test-js"))
            }
        }
        val wasmTest by getting {
            languageSettings {
                optIn("org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi")
                optIn("org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi")
            }
            dependsOn(jsWasmTest)
        }

        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

project.afterEvaluate {
    //Disable jsWasmMain intermediate sourceset publication
    tasks.named("compileJsWasmMainKotlinMetadata") {
        enabled = false
    }
}
