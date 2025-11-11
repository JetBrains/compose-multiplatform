plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    js("webJs") {
        browser { }
        binaries.executable()
    }

    wasmJs("webWasm") {
        browser { }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:COMPOSE_VERSION_PLACEHOLDER")
        }

        val webMain by creating { dependsOn(commonMain.get()) }
        val webJsMain by getting { dependsOn(webMain) }
        val webWasmMain by getting { dependsOn(webMain) }
    }
}
