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
                    useConfigDirectory("${rootProject.projectDir}/test-utils/conf/karma.config.common.d")
                    useChromeHeadless()
                    useFirefox()
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
                implementation(kotlin("stdlib-js"))
                implementation(npm("css-typed-om", "0.4.0"))

                implementation(project(":web-core"))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}
