import org.jetbrains.compose.gradle.standardConf

val composeHtmlEapKotlinxBrowserCommonSubsetVersion: String by rootProject.extra

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
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
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(libs.kotlinx.coroutines.core)
                implementation(
                    "org.jetbrains.compose.html:kotlinx-browser-common-subset:" +
                        composeHtmlEapKotlinxBrowserCommonSubsetVersion
                )
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}
