plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("maven-publish")
}

group = "me.sample"
version = "1.0"

kotlin {
    js(providers.gradleProperty("jsTargetName").getOrElse("js")) {
        browser()
    }
    wasmJs {
        browser()
    }
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.ui:ui:COMPOSE_VERSION_PLACEHOLDER")
        }
    }
}

publishing {
    repositories {
        maven {
            name = "Test"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}
