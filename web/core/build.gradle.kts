import org.jetbrains.compose.gradle.standardConf

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

val skikoWasm by configurations.creating

dependencies {
    skikoWasm("org.jetbrains.skiko:skiko-js-wasm-runtime:0.0.0-SNAPSHOT")
}

val copySkikoResources = tasks.register("copySkikoResources", Copy::class) {
    from(skikoWasm.map { zipTree(it) }) {
        include("skiko.wasm")
        include("skiko.js")
    }
    destinationDir = file("${project.buildDir}/skiko")
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest>().configureEach {
    dependsOn(copySkikoResources)
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
