import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

enum class OS {
    IOS,
    WINDOWS,
    LINUX,
    MACOS,
    BROWSER
}

enum class ARCH {
    X64,
    ARM64,
    JS
}

enum class BACKEND {
    JVM,
    NATIVE,
    JS
}

data class Platform(val os: OS, val arch: ARCH, val backend: BACKEND)

val JVM_ALL = listOf(
    Platform(OS.LINUX, ARCH.X64, BACKEND.JVM),
    Platform(OS.WINDOWS, ARCH.X64, BACKEND.JVM),
    Platform(OS.MACOS, ARCH.X64, BACKEND.JVM),
    Platform(OS.MACOS, ARCH.ARM64, BACKEND.JVM)
)

val MACOS_ALL = listOf(
    Platform(OS.MACOS, ARCH.X64, BACKEND.NATIVE),
    Platform(OS.MACOS, ARCH.ARM64, BACKEND.NATIVE)
)

val IOS_ALL = listOf(
    Platform(OS.IOS, ARCH.X64, BACKEND.NATIVE),
    Platform(OS.IOS, ARCH.ARM64, BACKEND.NATIVE)
)

val WEB = Platform(OS.BROWSER, ARCH.JS, BACKEND.JS)

fun org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget.makeNativeDarwinTarget(entry: String) {
    binaries {
        executable {
            entryPoint = entry
            freeCompilerArgs += listOf(
                "-linker-option", "-framework", "-linker-option", "Metal",
                "-linker-option", "-framework", "-linker-option", "CoreText",
                "-linker-option", "-framework", "-linker-option", "CoreGraphics"
            )
        }
    }
}

val allSourceSets = mutableMapOf<Platform, org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet>()

fun org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension.kapp(
    entryPoint: String,
    platforms: List<Platform>
) {
    platforms.groupBy { it.backend }.forEach {
        when (it.key) {
            BACKEND.JVM -> {
                jvm("desktop")
            }
            BACKEND.JS -> {
                js(IR) {
                    browser()
                    binaries.executable()
                }
            }
            BACKEND.NATIVE -> {
                it.value.forEach { platform ->
                    when {
                        platform.os == OS.MACOS && platform.arch == ARCH.X64 -> {
                            macosX64 {
                                makeNativeDarwinTarget(entryPoint)
                            }
                        }
                        platform.os == OS.MACOS && platform.arch == ARCH.ARM64 -> {
                            macosArm64 {
                                makeNativeDarwinTarget(entryPoint)
                            }
                        }
                        platform.os == OS.IOS && platform.arch == ARCH.X64 -> {
                            iosX64("uikitX64") {
                                makeNativeDarwinTarget(entryPoint)
                            }
                        }
                        platform.os == OS.IOS && platform.arch == ARCH.ARM64 -> {
                            iosArm64("uikitArm64") {
                                makeNativeDarwinTarget(entryPoint)
                            }
                        }
                    }
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
        platforms.groupBy { it.backend }.forEach {
            when (it.key) {
                BACKEND.JVM -> {
                    val desktopMain by getting {
                        dependsOn(commonMain)
                        dependencies {
                            implementation(compose.desktop.currentOs)
                        }
                    }
                    it.value.forEach { platform ->
                        allSourceSets[platform] = desktopMain
                    }

                }
                BACKEND.JS -> {
                    val jsMain by getting {
                        dependsOn(commonMain)
                    }
                    it.value.forEach { platform ->
                        allSourceSets[platform] = jsMain
                    }
                }
                BACKEND.NATIVE -> {
                    val darwinMain by creating {
                        dependsOn(commonMain)
                    }
                    val macosMain by creating {
                        dependsOn(darwinMain)
                    }
                    val uikitMain by creating {
                        dependsOn(darwinMain)
                    }
                    it.value.forEach { platform ->
                        when {
                            platform.os == OS.MACOS && platform.arch == ARCH.X64 -> {
                                val macosX64Main by getting {
                                    dependsOn(macosMain)
                                }
                                allSourceSets[platform] = macosX64Main
                            }
                            platform.os == OS.MACOS && platform.arch == ARCH.ARM64 -> {
                                val macosArm64Main by getting {
                                    dependsOn(macosMain)
                                }
                                allSourceSets[platform] = macosArm64Main
                            }
                            platform.os == OS.IOS && platform.arch == ARCH.X64 -> {
                                val uikitX64Main by getting {
                                    dependsOn(uikitMain)
                                }
                                allSourceSets[platform] = uikitX64Main
                            }
                            platform.os == OS.IOS && platform.arch == ARCH.ARM64 -> {
                                val uikitArm64Main by getting {
                                    dependsOn(uikitMain)
                                }
                                allSourceSets[platform] = uikitArm64Main
                            }
                        }
                    }
                }
            }
        }
    }

    // TODO: remove those two.
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }

    rootProject.extensions.configure<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension> {
        versions.webpackDevServer.version = "4.0.0"
        versions.webpackCli.version = "4.9.0"
        nodeVersion = "16.0.0"
    }
}

kotlin {
    kapp(
        entryPoint = "org.jetbrains.compose.kapp.demo.main",
        JVM_ALL + WEB + MACOS_ALL + IOS_ALL
    )
}


// TODO: ensure that those two are also configured by `kapp` function.
compose.desktop {
    application {
        mainClass = "org.jetbrains.compose.kapp.demo.Simple_desktopKt"
    }
}

compose.experimental {
    web.application
    uikit.application
}
