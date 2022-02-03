import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

val withNative = true 

kotlin {
    jvm("desktop")
    js(IR) {
        browser()
        binaries.executable()
    }

    if (withNative) {
        iosX64("uikitX64") {
            binaries {
                executable {
                    entryPoint = "main"
                    freeCompilerArgs += listOf(
                        "-linker-option", "-framework", "-linker-option", "Metal",
                        "-linker-option", "-framework", "-linker-option", "CoreText",
                        "-linker-option", "-framework", "-linker-option", "CoreGraphics"
                    )
                }
            }
        }
        iosArm64("uikitArm64") {
            binaries {
                executable {
                    entryPoint = "main"
                    freeCompilerArgs += listOf(
                        "-linker-option", "-framework", "-linker-option", "Metal",
                        "-linker-option", "-framework", "-linker-option", "CoreText",
                        "-linker-option", "-framework", "-linker-option", "CoreGraphics"
                    )
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":Kapp:library"))
            }
        }

        val desktopMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        val jsMain by getting

        if (withNative) {
            val uikitMain by creating {
                dependsOn(commonMain)
            }
            val uikitX64Main by getting {
                dependsOn(uikitMain)
            }
            val uikitArm64Main by getting {
                dependsOn(uikitMain)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.jetbrains.compose.kapp.demo.Simple_desktopKt"
    }
}

compose.experimental {
    web.application
    if (withNative)
        uikit.application
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

afterEvaluate {
    rootProject.extensions.configure<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension> {
        versions.webpackDevServer.version = "4.0.0"
        versions.webpackCli.version = "4.9.0"
        nodeVersion = "16.0.0"
    }
}
