import kotlinx.benchmark.gradle.JvmBenchmarkTarget
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlinx.benchmark") version "0.4.15"
}

val composeVersion = providers.gradleProperty("compose.version").get()
val browserVersion = providers.gradleProperty("compose.html.eap.kotlinx-browser-common-subset.version").get()
val benchmarkVersion = "0.4.15"

dependencies {
    implementation(project(":html-core-eap"))
    implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
    implementation("org.jetbrains.compose.html:kotlinx-browser-common-subset:$browserVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:$benchmarkVersion")
}

// Allow the benchmark to access renderer internals.
tasks.named<KotlinCompile>("compileKotlin") {
    friendPaths.from(
        project(":html-core-eap").tasks.named<Jar>("jvmJar")
            .flatMap { it.archiveFile }
    )
}

// Match the library's Java compatibility.
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

benchmark {
    configurations {
        configureEach {
            // Preserve @Fork rather than the runner's default of one fork.
            advanced("jvmForks", "definedByJmh")
        }
        register("smoke") {
            warmups = 1
            iterations = 1
            iterationTime = 100
            iterationTimeUnit = "ms"
        }
    }
    targets {
        register("main") {
            this as JvmBenchmarkTarget
            jmhVersion = "1.37"
        }
    }
}
