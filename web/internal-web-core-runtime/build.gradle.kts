import org.jetbrains.compose.gradle.standardConf

plugins {
    kotlin("multiplatform")
    //id("org.jetbrains.compose")
}

kotlin {
    js(IR) {
        browser() {
//            testTask {
//                useKarma {
//                    standardConf()
//                }
//            }
        }
    }
    wasm {
        browser() {

        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                //implementation(comp core.runtime)
                implementation("org.jetbrains.compose.runtime:runtime:1.3.0-rc01")
                //implementation("org.jetbrains.compose.web:web-core:1.2.0-SNAPSHOT")
            }
        }

        val jsWasmMain by creating { }

        val jsMain by getting {
            dependsOn(jsWasmMain)
        }

        val wasmMain by getting {
            dependsOn(jsWasmMain)
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        val wasmTest by getting {
            dependencies {
                implementation(kotlin("test-wasm"))
            }
        }
    }
}

project.tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xklib-enable-signature-clash-checks=false",
        "-Xplugin=${project.properties["compose.plugin.path"]}"
    )
}