plugins {
    kotlin("multiplatform") version "1.5.10"
    id("org.jetbrains.compose") version "0.5.0-build221"
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
                implementation(compose.web.core)
                implementation(compose.runtime)
            }
        }
    }
}
