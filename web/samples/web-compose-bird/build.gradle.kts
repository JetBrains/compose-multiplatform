// Add compose gradle plugin
plugins {
    kotlin("multiplatform") version "1.5.21"
    id("org.jetbrains.compose")
}
group = "com.theapache64.composebird"
version = "1.0.0-alpha01"

// Add maven repositories
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
            dependencies {
                implementation(project(":web-core"))
                implementation(compose.runtime)
            }
        }
    }
}
