import org.jetbrains.compose.gradle.standardConf

plugins {
    kotlin("multiplatform")
    ////id("org.jetbrains.compose")
}


kotlin {
//    jvm {
//
//    }
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
    wasm {
        browser() {
            testTask {
                useKarma {
                    standardConf()
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:1.3.0-rc01")
                //implementation(compose.runtime)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(project(":test-utils"))
                implementation(kotlin("test"))
            }
        }

        val jsWasmMain by creating {
            dependsOn(commonMain)
            languageSettings {
                optIn("org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi")
            }
            dependencies {
                implementation(project(":internal-web-core-runtime"))
                // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect
            }
        }

        val jsMain by getting {
            languageSettings {
                optIn("org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi")
            }
            dependsOn(jsWasmMain)
        }

        val wasmMain by getting {
            languageSettings {
                optIn("org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi")
            }
            dependsOn(jsWasmMain)
        }

        val jsTest by getting {
            languageSettings {
                optIn("org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi")
                optIn("org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi")
            }
        }

//        val jvmMain by getting {
//            dependencies {
//                implementation("org.jetbrains.compose.desktop:desktop-jvm-macos-arm64")
//            }
//        }
    }
}

project.tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xklib-enable-signature-clash-checks=false",
        "-Xplugin=${project.properties["compose.plugin.path"]}"
    )
}