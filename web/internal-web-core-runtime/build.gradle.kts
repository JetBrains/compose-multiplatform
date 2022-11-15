import org.jetbrains.compose.gradle.standardConf

plugins {
    kotlin("multiplatform")
//    id("org.jetbrains.compose")
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
                implementation("org.jetbrains.compose.runtime:runtime:0.0.1-SNAPSHOT")
//                implementation(compose.runtime)
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

project.tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xklib-enable-signature-clash-checks=false",
        "-Xplugin=${project.properties["compose.plugin.path"]}"
    )
}
