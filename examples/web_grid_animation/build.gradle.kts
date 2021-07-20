plugins {
    kotlin("multiplatform") version "1.5.10"
    id("org.jetbrains.compose") version "0.5.0-build235"
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
                implementation(project(":web-core"))
                implementation(compose.runtime)
            }
        }
    }
}
