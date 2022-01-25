import org.jetbrains.compose.gradle.standardConf

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}


kotlin {
    js(IR) {
        compilations.getByName("test") {
            kotlinOptions {
                freeCompilerArgs += "-opt-in=org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi"
            }
        }

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
                implementation(project(":internal-web-core-runtime"))
                implementation(kotlin("stdlib-js"))
                implementation(project(":web-core"))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(project(":test-utils"))
                implementation(kotlin("test-js"))
            }
        }
    }
}
