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
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
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
       it + listOf("versionInfo=\"$composeVersion (Kotlin $kotlinVersion)\"")
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
