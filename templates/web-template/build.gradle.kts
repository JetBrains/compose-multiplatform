import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    // __KOTLIN_COMPOSE_VERSION__
    kotlin("multiplatform") version "1.5.21"
    // __LATEST_COMPOSE_RELEASE_VERSION__
    id("org.jetbrains.compose") version ("0.5.0-build270")
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            kotlin.srcDir("src/main/kotlin")
            resources.srcDir("src/main/resources")

            dependencies {
                implementation(compose.web.core)
                implementation(compose.runtime)
            }
        }
    }
}
