import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm("desktop")
    macosX64 {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    macosArm64 {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }

    wasmJs {
        binaries.executable()
        browser {
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.runtime)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.1")
            }
        }

        val nativeMain by creating {
            dependsOn(commonMain)
        }
        val macosMain by creating {
            dependsOn(nativeMain)
        }
        val macosX64Main by getting {
            dependsOn(macosMain)
        }
        val macosArm64Main by getting {
            dependsOn(macosMain)
        }
    }
}

compose.desktop {
    application {
        mainClass = "Main_desktopKt"
    }
}

val runArguments: String? by project

// Handle runArguments property
gradle.taskGraph.whenReady {
    tasks.named<JavaExec>("run") {
        args(runArguments?.split(" ") ?: listOf<String>())
    }
    tasks.forEach { t ->
        if ((t is Exec) && t.name.startsWith("runReleaseExecutableMacos")) {
            t.args(runArguments?.split(" ") ?: listOf<String>())
        }
    }
    tasks.named<KotlinWebpack>("wasmJsBrowserProductionRun") {
        val args = runArguments?.split(" ")
            ?.mapIndexed { index, arg -> "arg$index=$arg" }
            ?.joinToString("&") ?: ""

        devServerProperty = devServerProperty.get().copy(
            open = "http://localhost:8080?$args"
        )
    }
}
