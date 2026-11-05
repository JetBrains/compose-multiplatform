import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin

import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
plugins {
	kotlin("multiplatform")
	kotlin("plugin.compose")
	id("org.jetbrains.compose")
}

repositories {
	mavenCentral()
	maven("https://packages.jetbrains.team/maven/p/cmp/dev")
	maven("https://packages.jetbrains.team/maven/p/kt/kotlin-js-wrappers")
    google()
}

kotlin {
	js(IR) {
		browser()
		binaries.executable()
	}
	sourceSets {
		val jsMain by getting {
			dependencies {
				implementation(compose.html.core)
				implementation(compose.runtime)
				implementation("org.jetbrains.kotlin-wrappers:kotlin-react:17.0.2-pre.201-kotlin-1.5.0")
				implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:17.0.2-pre.201-kotlin-1.5.0")
				implementation("org.jetbrains.kotlin-wrappers:kotlin-styled:5.3.0-pre.201-kotlin-1.5.0")
				implementation(npm("react", "17.0.2"))
				implementation(npm("react-dom", "17.0.2"))
				implementation(npm("react-youtube-lite", "1.0.1"))
			}
		}
	}
}

allprojects {
    val subproject = this
    // Override the node version to fix https://youtrack.jetbrains.com/issue/CMP-11011
    // Workaround is from https://youtrack.jetbrains.com/issue/KT-90732
    subproject.plugins.withType<NodeJsPlugin>().configureEach {
        subproject.extensions.getByType(NodeJsEnvSpec::class.java).version.set(libs.versions.node)
    }
    subproject.plugins.withType<WasmNodeJsPlugin>().configureEach {
        subproject.extensions.getByType(WasmNodeJsEnvSpec::class.java).version.set(libs.versions.node)
    }
}
