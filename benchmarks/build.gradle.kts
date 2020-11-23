import org.jetbrains.compose.compose
import kotlinx.benchmark.gradle.*
import org.jetbrains.kotlin.allopen.gradle.*

plugins {
    // __KOTLIN_COMPOSE_VERSION__
    kotlin("jvm") version "1.4.20"
    // __LATEST_COMPOSE_RELEASE_VERSION__
    id("org.jetbrains.compose") version "0.2.0-build128"
    // __KOTLIN_COMPOSE_VERSION__
    kotlin("plugin.allopen") version "1.4.20"
    id("kotlinx.benchmark") version "0.2.0-dev-20"
}

repositories {
    jcenter()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://dl.bintray.com/kotlin/kotlinx")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx.benchmark.runtime:0.2.0-dev-20")
}

configure<AllOpenExtension> {
    annotation("org.openjdk.jmh.annotations.State")
}

benchmark {
    configurations {
        named("main") {
            iterationTime = 5
            iterationTimeUnit = "sec"

        }
    }
    targets {
        register("main") {
            this as JvmBenchmarkTarget
            jmhVersion = "1.21"
        }
    }
}
