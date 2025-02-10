import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import kotlin.text.replace

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

val skikoVersion = project.properties["skiko.version"] as? String

kotlin {
    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "benchmarks"
            isStatic = true
        }
    }

    listOf(
        macosX64(),
        macosArm64()
    ).forEach { macosTarget ->
        macosTarget.binaries {
            executable {
                entryPoint = "main"
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        binaries.executable()
        browser ()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.ui) {
                    exclude("org.jetbrains.skiko")
                }
                implementation(compose.foundation) {
                    exclude("org.jetbrains.skiko")
                }
                implementation(compose.material) {
                    exclude("org.jetbrains.skiko")
                }
                implementation(compose.runtime) {
                    exclude("org.jetbrains.skiko")
                }
                implementation(compose.components.resources) {
                    exclude("org.jetbrains.skiko")
                }
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs) {
                    exclude("org.jetbrains.compose.ui")
                    exclude("org.jetbrains.compose.foundation")
                    exclude("org.jetbrains.compose.material")
                    exclude("org.jetbrains.skiko")
                }
                implementation("org.jetbrains.skiko:skiko:$skikoVersion")
                implementation("org.jetbrains.skiko:skiko-awt:$skikoVersion")
                implementation("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:$skikoVersion")
                runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.1")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "Main_desktopKt"
    }
}

val runArguments: String? by project
val composeVersion: String? = project.properties["compose.version"] as? String
val kotlinVersion: String? = project.properties["kotlin.version"] as? String
var appArgs = runArguments
    ?.split(" ")
    .orEmpty().let {
       it + listOf("versionInfo=\"$composeVersion (Skiko $skikoVersion)\"")
    }
    .map {
        it.replace(" ", "%20")
    }

println("runArguments: $appArgs")

// Handle runArguments property
gradle.taskGraph.whenReady {
    tasks.named<JavaExec>("run") {
        args(appArgs)
    }
    tasks.forEach { t ->
        if ((t is Exec) && t.name.startsWith("runReleaseExecutableMacos")) {
            t.args(appArgs)
        }
    }
    tasks.named<KotlinWebpack>("wasmJsBrowserProductionRun") {
        val args = appArgs
            .mapIndexed { index, arg -> "arg$index=$arg" }
            .joinToString("&")

        devServerProperty = devServerProperty.get().copy(
            open = "http://localhost:8080?$args"
        )
    }
}
