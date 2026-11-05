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
				implementation(project.dependencies.platform("org.jetbrains.kotlin-wrappers:kotlin-wrappers-bom:2026.10.5"))
				implementation("org.jetbrains.kotlin-wrappers:kotlin-react")
				implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom")
				implementation("org.jetbrains.kotlin-wrappers:kotlin-emotion-react")
				implementation(npm("react-youtube-lite", "1.0.1"))
			}
		}
	}
}

