@file:OptIn(org.jetbrains.compose.ExperimentalComposeApi::class)

import org.jetbrains.compose.dsl.RenderingBackend

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    iosArm64()
    iosSimulatorArm64()
    sourceSets.commonMain.dependencies {
        implementation("org.jetbrains.compose.ui:ui:COMPOSE_VERSION_PLACEHOLDER")
    }
}

providers.gradleProperty("backend").orNull?.let { backend ->
    compose.ios {
        renderingBackend.set(RenderingBackend.valueOf(backend))
    }
}
