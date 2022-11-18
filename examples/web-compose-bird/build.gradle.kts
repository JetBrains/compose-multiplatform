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
    google()
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    wasm {
        d8()
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
//                implementation(compose.web.core)
//                implementation(compose.runtime)
                implementation("org.jetbrains.compose.web:web-core-js:1.2.0-SNAPSHOT")
                implementation("org.jetbrains.compose.web:internal-web-core-runtime-js:1.2.0-SNAPSHOT")
                implementation("org.jetbrains.compose.runtime:runtime-js:1.3.0-rc01")
            }
        }
        val wasmMain by getting {
            dependencies {
//                implementation(compose.web.core)
//                implementation(compose.runtime)
                implementation("org.jetbrains.compose.web:web-core-wasm:1.2.0-SNAPSHOT")
                implementation("org.jetbrains.compose.web:internal-web-core-runtime-wasm:1.2.0-SNAPSHOT")
                implementation("org.jetbrains.compose.runtime:runtime-wasm:1.3.0-rc01")
            }
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