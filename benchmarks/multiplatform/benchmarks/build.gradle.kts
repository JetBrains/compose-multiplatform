import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.d8.D8Exec
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import kotlin.text.replace

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
        d8 {
            compilerOptions.freeCompilerArgs.add("-Xwasm-attach-js-exception")
            runTask {
                // It aborts even on coroutine cancellation exceptions:
                // d8Args.add("--abort-on-uncaught-exception")
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
                implementation("org.jetbrains.compose.material:material-icons-core:1.6.11")
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.io)
                implementation(libs.kotlinx.datetime)
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

val composeVersion = libs.versions.compose.multiplatform
val kotlinVersion = libs.versions.kotlin

// Handle runArguments property
gradle.taskGraph.whenReady {
    var appArgs = runArguments
        ?.split(" ")
        .orEmpty().let {
            it + listOf("versionInfo=\"${composeVersion.get()} (Kotlin ${kotlinVersion.get()})\"")
        }
        .map {
            it.replace(" ", "%20")
        }

    println("runArguments: $appArgs")

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

    @OptIn(ExperimentalWasmDsl::class)
    tasks.withType<D8Exec>().configureEach {
        inputFileProperty.set(rootProject.layout.buildDirectory.file(
            "js/packages/compose-benchmarks-benchmarks-wasm-js/kotlin/launcher.mjs")
        )

        args(appArgs)
    }
}


tasks.register("buildD8Distribution", Zip::class.java) {
    dependsOn("wasmJsProductionExecutableCompileSync")
    from(rootProject.layout.buildDirectory.file("js/packages/compose-benchmarks-benchmarks-wasm-js/kotlin"))
    archiveFileName.set("d8-distribution.zip")
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("distributions"))
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenExec>().configureEach {
    binaryenArgs.add("-g") // keep the readable names
}

@OptIn(ExperimentalWasmDsl::class)
rootProject.the<BinaryenRootEnvSpec>().apply {
    // version = "122" // change only if needed
}

val jsOrWasmRegex = Regex("js|wasm")

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group.startsWith("org.jetbrains.skiko") &&
            jsOrWasmRegex.containsMatchIn(requested.name)
        ) {
            // to keep the readable names from Skiko
            useVersion(requested.version!! + "+profiling")
        }
    }
}
