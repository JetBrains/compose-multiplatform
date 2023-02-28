import org.jetbrains.compose.gradle.standardConf

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
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
        binaries.executable()
    }

    wasm { browser() }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(kotlin("stdlib-common"))
            }
        }

        val jsWasmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(project(":internal-web-core-runtime"))
                implementation(kotlin("stdlib-common"))
                implementation(project(":web-core"))
            }
        }

        val jsMain by getting {
            dependsOn(jsWasmMain)
        }

        val wasmMain by getting {
            dependsOn(jsWasmMain)
        }

        val jsWasmTest by creating {
            dependencies {
                implementation(project(":test-utils"))
                implementation(kotlin("test"))
            }
        }
        val jsTest by getting {
            languageSettings {
                optIn("org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi")
            }
            dependsOn(jsWasmTest)
        }

        val wasmTest by getting {
            languageSettings {
                optIn("org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi")
            }
            dependsOn(jsWasmTest)
        }
    }
}
