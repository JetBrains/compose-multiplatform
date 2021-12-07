plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}


kotlin {
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
            }
        }
    }
}
