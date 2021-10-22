plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}


kotlin {
    js(IR) {
        browser() {
            testTask {
                testLogging.showStandardStreams = true
                useKarma {
                    useChromeHeadless()
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

        all {
            languageSettings {
                useExperimentalAnnotation("org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi")
            }
        }
    }
}
