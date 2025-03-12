import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
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
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.runtime)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.io)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                runtimeOnly(libs.kotlinx.coroutines.swing)
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
