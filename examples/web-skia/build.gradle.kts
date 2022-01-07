import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

val SKIKO_VERSION: String by project

val skikoWasm by configurations.creating

dependencies {
    skikoWasm("org.jetbrains.skiko:skiko-js-wasm-runtime:${SKIKO_VERSION}")
}

val skikoResourcesDir = "${rootProject.buildDir}/skiko"
val copySkikoResources by tasks.registering(Copy::class) {
    from(skikoWasm.map { zipTree(it) }) {
        include("skiko.wasm")
        include("skiko.js")
    }
    destinationDir = file(skikoResourcesDir)
}

tasks.withType<ProcessResources> {
    dependsOn(copySkikoResources)
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            resources.srcDirs(skikoResourcesDir)

            dependencies {
                implementation(compose.web.core)
                implementation(compose.web.svg)
                implementation(compose.runtime)
            }
        }
    }
}

// a temporary workaround for a bug in jsRun invocation - see https://youtrack.jetbrains.com/issue/KT-48273
afterEvaluate {
    rootProject.extensions.configure<NodeJsRootExtension> {
        versions.webpackDevServer.version = "4.0.0"
        versions.webpackCli.version = "4.9.0"
    }
}
