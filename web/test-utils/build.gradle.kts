import org.jetbrains.compose.gradle.standardConf

plugins {
    kotlin("multiplatform")
    //id("org.jetbrains.compose")
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
                implementation(kotlin("stdlib-common"))
                implementation(kotlin("test"))
                implementation("org.jetbrains.compose.runtime:runtime:1.3.0-rc01")
//                implementation(project(":internal-web-core-runtime"))
            }
        }

        val wasmJsMain by creating {
            dependsOn(commonMain)
        }

        val wasmJsTest by creating {
            dependsOn(wasmJsMain)
        }

        val jsMain by getting {
            dependsOn(wasmJsMain)
            dependencies {
                implementation("org.jetbrains.compose.web:web-core-js:1.2.0-SNAPSHOT")
                implementation("org.jetbrains.compose.web:internal-web-core-runtime-js:1.2.0-SNAPSHOT")
                implementation("org.jetbrains.compose.runtime:runtime-js:1.3.0-rc01")
            }
        }

        val wasmMain by getting {
            dependsOn(wasmJsMain)
            dependencies {
                implementation("org.jetbrains.compose.web:web-core-wasm:1.2.0-SNAPSHOT")
                implementation("org.jetbrains.compose.web:internal-web-core-runtime-wasm:1.2.0-SNAPSHOT")
                implementation("org.jetbrains.compose.runtime:runtime-wasm:1.3.0-rc01")
            }
        }
        val jsTest by getting {
            dependsOn(wasmJsTest)
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        val wasmTest by getting {
            dependsOn(wasmJsTest)
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