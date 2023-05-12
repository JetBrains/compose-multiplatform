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
    }
    wasm {
        browser {}
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-RC-wasm0")
            }
        }

        val jsWasmMain by creating {
            dependsOn(commonMain)
        }

        val jsMain by getting {
            dependsOn(jsWasmMain)
        }

        val wasmMain by getting {
            dependsOn(jsWasmMain)
        }

        val jsWasmTest by creating {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jsTest by getting {
            dependsOn(jsWasmTest)
        }

        val wasmTest by getting {
            dependsOn(jsWasmTest)
        }
    }
}