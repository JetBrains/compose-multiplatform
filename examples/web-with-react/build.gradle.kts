plugins {
	kotlin("multiplatform") version "1.4.32"
	id("org.jetbrains.compose") version "0.0.0-web-dev-12"
}

repositories {
	mavenCentral()
	maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
	maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers")
}

kotlin {
	js(IR) {
		browser()
		binaries.executable()
	}
	sourceSets {
		val jsMain by getting {
			dependencies {
				implementation(compose.web.web)
				implementation(compose.runtime)
				implementation("org.jetbrains:kotlin-react:17.0.2-pre.155-kotlin-1.4.32")
				implementation("org.jetbrains:kotlin-react-dom:17.0.2-pre.155-kotlin-1.4.32")
				implementation(npm("react", "17.0.2"))
				implementation(npm("react-dom", "17.0.2"))
				implementation(npm("react-youtube-lite", "1.0.1"))
			}
		}
	}
}
