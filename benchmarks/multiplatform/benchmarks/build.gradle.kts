import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.d8.D8Exec
import org.jetbrains.kotlin.gradle.targets.js.d8.D8Plugin.Companion.kotlinD8RootExtension
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    mavenLocal()
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
        d8 {
            runTask {
                d8Args.add("--abort-on-uncaught-exception")
//                d8Args.add("--print-all-exceptions")
            }
        }
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.runtime)
                implementation(compose.components.resources)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")
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


configurations.all {
    resolutionStrategy.eachDependency {
        val groupPrefix = "org.jetbrains.skiko"
        val group = requested.group

        if (
            group.startsWith(groupPrefix)) {
            useVersion("25.2.5-SNAPSHOT")
        }
    }
}

tasks.withType<D8Exec>().configureEach {
    doFirst {
        val file = rootProject.layout.buildDirectory.file(
            "js/packages/compose-benchmarks-benchmarks-wasm-js/kotlin/compose-benchmarks-benchmarks-wasm-js.mjs"
        ).get().asFile
        file.appendText("\nawait import('./polyfills.mjs');\n")
    }
}

tasks.register("buildD8Distribution", Zip::class.java) {
    from(rootProject.layout.buildDirectory.file("js/packages/compose-benchmarks-benchmarks-wasm-js/kotlin"))
    archiveFileName.set("d8-distribution.zip")
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("distributions"))
}