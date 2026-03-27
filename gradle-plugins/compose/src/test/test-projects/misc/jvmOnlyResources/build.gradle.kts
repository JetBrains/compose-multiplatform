plugins {
    id("org.jetbrains.compose")
    kotlin("plugin.compose")
    kotlin("jvm")
}

group = "me.app"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.components:components-resources:COMPOSE_VERSION_PLACEHOLDER")
}

