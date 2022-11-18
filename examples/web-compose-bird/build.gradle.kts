import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension

plugins {
    kotlin("multiplatform")
    //id("org.jetbrains.compose")
}
group = "com.theapache64.composebird"
version = "1.0.0-alpha01"

// Add maven repositories
repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
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
//                implementation(compose.web.core)
//                implementation(compose.runtime)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4-wasm0")
                implementation("org.jetbrains.compose.web:web-core:1.2.0-SNAPSHOT")
                implementation("org.jetbrains.compose.web:internal-web-core-runtime:1.2.0-SNAPSHOT")
                implementation("org.jetbrains.compose.runtime:runtime:1.3.0-rc01-SNAPSHOT")
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

project.tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xklib-enable-signature-clash-checks=false",
        "-Xplugin=${project.properties["compose.plugin.path"]}",
        "-Xir-dce"
    )
}