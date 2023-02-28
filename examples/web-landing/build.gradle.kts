import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
    maven("https://packages.jetbrains.team/maven/p/karpovich-sandbox/ksandbox")
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    wasm {
        browser()
        binaries.executable()
    }

    sourceSets {
        val jsWasmMain by creating {
            dependencies {
                //implementation(npm("highlight.js", "10.7.2"))
                implementation(compose.web.core)
                implementation(compose.runtime)
            }
        }
        val jsMain by getting {
            dependsOn(jsWasmMain)
        }
        val wasmMain by getting {
            dependsOn(jsWasmMain)
        }
    }
}

// a temporary workaround for a bug in jsRun invocation - see https://youtrack.jetbrains.com/issue/KT-48273
afterEvaluate {
    rootProject.extensions.configure<NodeJsRootExtension> {
        versions.webpackDevServer.version = "4.0.0"
        versions.webpackCli.version = "4.10.0"
    }
}
