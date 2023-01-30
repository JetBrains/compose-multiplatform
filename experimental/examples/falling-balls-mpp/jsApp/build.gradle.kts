import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
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
        val jsMain by getting  {
            dependencies {
                implementation(project(":shared"))
            }
        }
        val wasmMain by getting {
            dependencies {
                implementation(project(":shared"))
            }
        }
    }
}

compose.experimental {
    web.application {}
}

compose.kotlinCompilerPlugin.set("23.1.27")
//compose.kotlinCompilerPluginArgs.add("suppressKotlinVersionCompatibilityCheck=1.8.20-dev-6044")

//tasks.withType<KotlinCompile<*>>() {
//    kotlinOptions.freeCompilerArgs +=
//        listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=1.8.20-dev-6044")
//}

