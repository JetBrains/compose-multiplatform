import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {

    jvm()

    listOf(
            macosX64(),
            macosArm64(),
    ).forEach {
        it.binaries {
            executable {
                entryPoint = "main"
                freeCompilerArgs += listOf(
                        "-linker-options",
                        "-framework",
                        "-linker-option",
                        "Metal",
                        "-Xdisable-phases=VerifyBitcode"
                )
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:COMPOSE_VERSION_PLACEHOLDER")
                implementation("org.jetbrains.compose.material:material:COMPOSE_VERSION_PLACEHOLDER")
            }
        }

        jvmMain {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.desktop {
    val projectName = "Test Layered Icon"
    application {
        mainClass = "MainKt"
        nativeDistributions {
            packageName = projectName
            packageVersion = "1.0.0"
            targetFormats(TargetFormat.Dmg)
            macOS {
                dockName = "CustomDockName"
                minimumSystemVersion = "12.0"
                iconFile.set(project.file("subdir/Kotlin_icon_big.icns"))
                layeredIconDir.set(project.file("subdir/kotlin_icon_big.icon"))
            }
        }
    }

    nativeApplication {

        targets(
                targets = kotlin.targets.filter {
                    it.platformType == KotlinPlatformType.native &&
                            it.name.contains("macos")
                }.toTypedArray()
        )
        distributions {
            targetFormats(TargetFormat.Dmg)
            macOS {
                packageName = projectName
                packageVersion = "1.0.0"
                iconFile.set(project.file("subdir/Kotlin_icon_big.icns"))
                layeredIconDir.set(project.file("subdir/kotlin_icon_big.icon"))
            }
        }
    }
}
