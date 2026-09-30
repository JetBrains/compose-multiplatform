import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Exec
import kotlin.text.replace

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidMultiplatformLibrary)
}

version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    google {
        url = uri("https://cache-redirector.jetbrains.com/dl.google.com/dl/android/maven2")
    }
    maven("https://packages.jetbrains.team/maven/p/cmp/dev")
    mavenCentral {
        url = uri("https://cache-redirector.jetbrains.com/maven-central")
    }
}

val composeVersion = libs.versions.compose.multiplatform

kotlin {
    jvm("desktop")

    android {
        namespace = "org.jetbrains.compose.benchmarks.shared"
        compileSdk = 37
        minSdk = 24
        androidResources.enable = true
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "benchmarks"
            isStatic = true
        }
    }

    macosArm64 {
        binaries {
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
                    freeCompilerArgs.add("-Xwasm-use-new-exception-proposal")
                }
            }
        }
    }

    js {
        browser()
        binaries.executable()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":compose-scene-impl"))

                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material)
                implementation(libs.compose.runtime)
                implementation(libs.compose.components.resources)

                implementation(libs.material.icons.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.io)
                implementation(libs.kotlinx.datetime)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }

        // Intermediate source set for all Skia/Skiko targets (non-Android)
        val skikoMain by creating {
            dependsOn(commonMain)
        }

        val desktopMain by getting {
            dependsOn(skikoMain)
            dependencies {
                // To be able to build both for the 1.12 and 1.13+ versions
                // we have to suppress deprecation. Otherwise, the execution of
                // benchmarks on the desktop target fail loading the dependencies.
                @Suppress("DEPRECATION")
                implementation(compose.desktop.currentOs)

                runtimeOnly(libs.kotlinx.coroutines.swing)
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.netty)
                implementation(libs.ktor.server.content.negotiation)
                implementation(libs.ktor.client.java)
                implementation(libs.ktor.server.cors)
            }
        }

        val appleMain by getting {
            dependsOn(skikoMain)
        }

        val webMain by getting {
            dependsOn(skikoMain)
            dependencies {
                implementation(libs.ktor.client.js)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.kotlinx.browser)
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

val kotlinVersion = libs.versions.kotlin

val jsJodaEsmFile = rootProject.layout.buildDirectory.file(
    "wasm/node_modules/@js-joda/core/dist/js-joda.esm.js"
)
val wasmResourcesDirectory = project.layout.buildDirectory.dir("processedResources/wasmJs/main")
val skikoRuntimeDirectory = project.layout.buildDirectory.dir(
    "compose/skiko-runtime-processed-wasmjs"
)

fun File.replaceRequired(oldValue: String, newValue: String) {
    check(isFile) { "Expected D8 runtime file does not exist: $this" }

    val original = readText()
    check(oldValue in original) {
        "Cannot prepare the D8 runtime: expected content was not found in $this"
    }
    writeText(original.replace(oldValue, newValue))
}

fun patchD8Runtime(directory: File) {
    directory.resolve("compose-benchmarks-benchmarks.import-object.mjs").apply {
        replaceRequired("from './skiko.mjs'", "from './skikod8.mjs'")
        replaceRequired("from '@js-joda/core'", "from './js-joda.esm.js'")
    }

    directory.resolve("skikod8.mjs").replaceRequired(
        oldValue = """
            export const awaitSkiko = loadSkikoWASM().then((module) => {
                loadedWasm._ = module.wasmExports;
                skikoGl = module.GL;
                return module
            });
        """.trimIndent(),
        newValue = """
            const _skikoWasmPath = String(import.meta.url).replace(/^file:\/\//, '').replace(/\/[^\/]+${'$'}/, '/skiko.wasm');
            const _skikoWasmBytes = read(_skikoWasmPath, 'binary');
            const _skikoWasmModule = loadSkikoWASM({
                instantiateWasm(info, receiveInstance) {
                    const instance = new WebAssembly.Instance(
                        new WebAssembly.Module(
                            _skikoWasmBytes instanceof Uint8Array
                                ? _skikoWasmBytes
                                : new Uint8Array(_skikoWasmBytes)
                        ),
                        info
                    );
                    receiveInstance(instance);
                    return instance.exports;
                }
            });
            export const awaitSkiko = _skikoWasmModule.then((module) => {
                loadedWasm._ = module.wasmExports;
                skikoGl = module.GL;
                return module
            });
        """.trimIndent()
    )
}

fun registerD8Runtime(variant: String, compiledPath: String, outputPath: String) =
    tasks.register<Sync>("prepareWasmJsD8${variant}Runtime") {
        dependsOn("wasmJs${variant}ExecutableCompileSync", ":kotlinWasmNpmInstall")
        from(project.layout.buildDirectory.dir(compiledPath))
        from(wasmResourcesDirectory)
        from(skikoRuntimeDirectory)
        from(jsJodaEsmFile)
        into(project.layout.buildDirectory.dir(outputPath))
        doLast {
            patchD8Runtime(destinationDir)
        }
    }

val d8DevelopmentDirectory = project.layout.buildDirectory.dir("d8/development")
val d8ProductionDirectory = project.layout.buildDirectory.dir("d8/production")
val prepareWasmJsD8DevelopmentRuntime = registerD8Runtime(
    variant = "Development",
    compiledPath = "compileSync/wasmJs/main/developmentExecutable/kotlin",
    outputPath = "d8/development"
)
val prepareWasmJsD8ProductionRuntime = registerD8Runtime(
    variant = "Production",
    compiledPath = "compileSync/wasmJs/main/productionExecutable/optimized",
    outputPath = "d8/production"
)

@OptIn(ExperimentalWasmDsl::class)
tasks.named<D8Exec>("wasmJsD8DevelopmentRun") {
    dependsOn(prepareWasmJsD8DevelopmentRuntime)
    inputFileProperty.set(d8DevelopmentDirectory.map { it.file("launcher.mjs") })
}

@OptIn(ExperimentalWasmDsl::class)
tasks.named<D8Exec>("wasmJsD8ProductionRun") {
    dependsOn(prepareWasmJsD8ProductionRuntime)
    inputFileProperty.set(d8ProductionDirectory.map { it.file("launcher.mjs") })
}

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
        args(appArgs)
    }
}


tasks.register("buildD8Distribution", Zip::class.java) {
    dependsOn(prepareWasmJsD8ProductionRuntime)
    from(d8ProductionDirectory)
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
    binaryenArguments.add("-g") // keep the readable names
}

@OptIn(ExperimentalWasmDsl::class)
project.the<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec>().apply {
    // version = "122" // change only if needed
}

val jsOrWasmRegex = Regex("js|wasm")

// not needed for now
/*
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
*/
