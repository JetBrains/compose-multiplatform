plugins {
    kotlin("multiplatform")// version "1.5.10"
    id("org.jetbrains.compose")// version (System.getenv("COMPOSE_INTEGRATION_VERSION") ?: "0.0.0-SNASPHOT")
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    js(IR) {
        nodejs {}
        browser() {}
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(compose.runtime)
            }
        }
    }
}
