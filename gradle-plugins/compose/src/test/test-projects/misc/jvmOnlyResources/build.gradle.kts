plugins {
    id("org.jetbrains.compose")
    kotlin("plugin.compose")
    kotlin("jvm")
}

group = "me.app"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.components.resources)
}

