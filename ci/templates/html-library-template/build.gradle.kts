plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

repositories {
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/cmp/dev")
    google()
}

kotlin {
    js {
        browser()
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            kotlin.srcDir("src/main/kotlin")
            resources.srcDir("src/main/resources")

            dependencies {
                implementation(libs.compose.html.core)
                implementation(libs.compose.runtime)
            }
        }
    }
}

