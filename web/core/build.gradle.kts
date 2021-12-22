import org.jetbrains.compose.gradle.standardConf

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

val skiko by configurations.creating

dependencies {
    skiko("org.jetbrains.skiko:skiko:0.0.0-SNAPSHOT")
}

tasks.register<Copy>("copySkiko") {
    from(skiko)
    into("${project.buildDir}/skiko")
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
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(kotlin("stdlib-common"))

                implementation("org.jetbrains.skiko:skiko:0.0.0-SNAPSHOT")
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
                implementation(project(":test-utils"))
                implementation(kotlin("test-js"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        all {
            languageSettings {
                useExperimentalAnnotation("org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi")
            }
        }
    }
}
