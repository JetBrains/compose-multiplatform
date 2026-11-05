@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import kotlinx.benchmark.gradle.JvmBenchmarkTarget
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose.html.lowering") apply false
    id("org.jetbrains.kotlinx.benchmark") version "0.4.15"
}

if (providers.gradleProperty("compose.html.lowering.enabled").orElse("false").get().toBooleanStrict()) {
    apply(plugin = "org.jetbrains.compose.html.lowering")
}

val composeVersion = providers.gradleProperty("compose.version").get()
val browserVersion = providers.gradleProperty("compose.html.eap.kotlinx-browser-common-subset.version").get()
val benchmarkVersion = "0.4.15"

kotlin {
    jvm()
    js {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }
    wasmJs {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }
    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":html-core-eap"))
            implementation(project(":html-svg-eap"))
            implementation("androidx.compose.runtime:runtime:$composeVersion")
            implementation("org.jetbrains.compose.html:kotlinx-browser-common-subset:$browserVersion")
        }
        jvmMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:$benchmarkVersion")
        }
        val webTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

// Allow the pooling benchmark to access renderer internals.
tasks.named<KotlinCompile>("compileKotlinJvm") {
    friendPaths.from(project(":html-core-eap").tasks.named<Jar>("jvmJar").flatMap { it.archiveFile })
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

benchmark {
    configurations {
        configureEach {
            providers.gradleProperty("benchmarks").orNull?.let { include(it) }
            advanced("jvmForks", "definedByJmh")
        }
        register("ssr") {
            val method = when (val mode = providers.gradleProperty("renderMode").orElse("string").get()) {
                "string" -> "renderToString"
                "writer" -> "streamToWriter"
                "callback" -> "streamToCallback"
                "all" -> ".*"
                else -> error("Unknown renderMode '$mode': use string, writer, callback or all")
            }
            include(".*SsrBenchmark.$method")
        }
        register("smoke") {
            warmups = 1
            iterations = 1
            iterationTime = 100
            iterationTimeUnit = "ms"
        }
    }
    targets {
        register("jvm") {
            this as JvmBenchmarkTarget
            jmhVersion = "1.37"
        }
    }
}

val hydrationFixtures = layout.buildDirectory.dir("generated/hydrationSources")
val jvmMain = kotlin.targets.getByName("jvm").compilations.getByName("main")
val generateHydrationFixtures = tasks.register<JavaExec>("generateHydrationFixtures") {
    group = "verification"
    dependsOn(jvmMain.compileTaskProvider)
    mainClass.set("benchmarks.hydration.GenerateHydrationFixturesKt")
    classpath(jvmMain.output.allOutputs, jvmMain.runtimeDependencyFiles)
    args(hydrationFixtures.get().asFile.absolutePath)
    outputs.dir(hydrationFixtures)
}

kotlin.sourceSets.named("webTest") {
    kotlin.srcDir(generateHydrationFixtures)
}

// Each browser benchmark invocation should collect fresh measurements.
listOf("jsBrowserTest", "wasmJsBrowserTest").forEach { name ->
    tasks.named(name) { outputs.upToDateWhen { false } }
}

tasks.register("hydrationBenchmark") {
    group = "benchmark"
    description = "Measures hydration on JS and Wasm in Chrome."
    dependsOn("jsBrowserTest", "wasmJsBrowserTest")
}
