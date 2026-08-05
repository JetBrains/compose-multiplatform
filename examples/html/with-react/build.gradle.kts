import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension

val reactVersion = extensions.getByType<VersionCatalogsExtension>()
	.named("libs")
	.findVersion("react")
	.get()
	.requiredVersion

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.composeCompiler)
	alias(libs.plugins.composeMultiplatform)
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
				implementation(libs.compose.html.core)
				implementation(libs.compose.runtime)
				implementation(libs.kotlin.react)
				implementation(libs.kotlin.react.dom)
				implementation(libs.kotlin.styled)
				implementation(npm("react", reactVersion))
				implementation(npm("react-dom", reactVersion))
				implementation(npm("react-youtube-lite", libs.versions.react.youtube.lite.get()))
			}
		}
	}
}
