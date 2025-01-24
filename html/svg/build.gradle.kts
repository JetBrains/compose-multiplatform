import org.jetbrains.compose.gradle.standardConf

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
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

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(kotlin("stdlib-common"))
            }
        }

        val jsMain by getting {
            dependencies {
                api(project(":internal-html-core-runtime"))
                implementation(kotlin("stdlib-js"))
                api(project(":html-core"))
            }
        }

        val jsTest by getting {
            languageSettings {
                optIn("org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi")
            }
            dependencies {
                implementation(project(":html-test-utils"))
                implementation(kotlin("test-js"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}
