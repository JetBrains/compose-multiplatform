plugins {
    val composeIntegrationVersion = (System.getenv("COMPOSE_INTEGRATION_VERSION")?.takeIf { it.isNotEmpty() } ?: "0.0.0-SNASPHOT")

    kotlin("multiplatform") version "1.5.10"
    id("org.jetbrains.compose") version composeIntegrationVersion
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
                implementation(project(":lib"))
            }
        }
    }
}
