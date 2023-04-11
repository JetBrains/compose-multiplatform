import org.jetbrains.compose.gradle.standardConf

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
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

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}
