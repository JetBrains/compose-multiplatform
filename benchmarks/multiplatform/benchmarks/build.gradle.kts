import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Exec
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

        binaries.configureEach {
            compilation.compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.apply {
                        add("-Xwasm-use-new-exception-proposal")
                    }
                }
            }
        }
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
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                runtimeOnly(libs.kotlinx.coroutines.swing)
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.netty)
                implementation(libs.ktor.server.content.negotiation)
                implementation(libs.ktor.client.java)
                implementation(libs.ktor.server.cors)
            }
        }

        val wasmJsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
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
            "wasm/packages/compose-benchmarks-benchmarks/kotlin/launcher.mjs")
        )

        args(appArgs)
    }
}


tasks.register("buildD8Distribution", Zip::class.java) {
    dependsOn("wasmJsProductionExecutableCompileSync")
    from(rootProject.layout.buildDirectory.file("wasm/packages/compose-benchmarks-benchmarks/kotlin"))
    archiveFileName.set("d8-distribution.zip")
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("distributions"))
}

tasks.register("runBrowserAndSaveStats") {
    fun printProcessOutput(inputStream: java.io.InputStream) {
        Thread {
            inputStream.bufferedReader().use { reader ->
                reader.lines().forEach { line ->
                    println(line)
                }
            }
        }.start()
    }

    fun runCommand(vararg command: String): Process {
        return ProcessBuilder(*command).start().also {
            printProcessOutput(it.inputStream)
            printProcessOutput(it.errorStream)
        }
    }

    doFirst {
        var serverProcess: Process? = null
        var clientProcess: Process? = null
        try {
            serverProcess = runCommand("./gradlew", "benchmarks:run",
                "-PrunArguments=runServer=true saveStatsToJSON=true")

            clientProcess = runCommand("./gradlew", "benchmarks:wasmJsBrowserProductionRun",
                "-PrunArguments=$runArguments saveStatsToJSON=true")

            serverProcess.waitFor()
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            serverProcess?.destroy()
            clientProcess?.destroy()
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenExec>().configureEach {
    binaryenArgs.add("-g") // keep the readable names
}

@OptIn(ExperimentalWasmDsl::class)
project.the<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec>().apply {
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
