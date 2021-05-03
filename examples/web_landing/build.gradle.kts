plugins {
    id("org.jetbrains.kotlin.multiplatform") version "1.4.32"
    id("org.jetbrains.compose") version "0.0.0-web-dev-10"
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(npm("highlight.js", "10.7.2"))
                implementation(compose.web.web)
                implementation(compose.runtime)
            }
        }
    }
}