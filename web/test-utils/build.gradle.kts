plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}


repositories {
    mavenCentral()
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
                implementation(kotlin("stdlib-common"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(project(":internal-web-core-runtime"))
                implementation(kotlin("stdlib-js"))
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}
