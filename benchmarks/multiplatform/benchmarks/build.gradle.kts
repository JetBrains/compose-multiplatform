import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.d8.D8Exec
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
            // compilerOptions.freeCompilerArgs.add("-Xwasm-use-new-exception-proposal")
            compilerOptions.freeCompilerArgs.add("-Xwasm-attach-js-exception")
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


val isWasmBuildForJetstream3 = project.hasProperty("wasm.jetstream3")
tasks.withType<D8Exec>().configureEach {
    doFirst {
        val distributionDir = rootProject.layout.buildDirectory.dir(
            "js/packages/compose-benchmarks-benchmarks-wasm-js/kotlin/"
        )
        val file = distributionDir.get().asFile.resolve("compose-benchmarks-benchmarks-wasm-js.mjs")
        file.appendText("\nawait import('./polyfills.mjs');\n")

        val newText = "\nglobalThis.isWasmBuildForJetstream3 = $isWasmBuildForJetstream3;\n" +
                "\nglobalThis.isD8 = true;\n" + file.readText()
        file.writeText(newText)

        // Use a special skiko mjs file for d8:
        val updText = file.readText()
            .replace("from './skiko.mjs';", "from './skikod8.mjs';")
        file.writeText(updText)
    }
}

tasks.register("buildD8Distribution", Zip::class.java) {
    from(rootProject.layout.buildDirectory.file("js/packages/compose-benchmarks-benchmarks-wasm-js/kotlin"))
    archiveFileName.set("d8-distribution.zip")
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("distributions"))
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenExec>().configureEach {
    binaryenArgs.add("-g") // keep the readable names
}

@OptIn(ExperimentalWasmDsl::class)
rootProject.the<BinaryenRootEnvSpec>().apply {
    version = "122"
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
