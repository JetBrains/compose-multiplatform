import org.jetbrains.compose.compose

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij")
    id("org.jetbrains.compose") version "1.0.1"
}

jvmTarget("11")

dependencies {
    api(compose.desktop.common)
    api(compose.desktop.macos_x64)
    api(compose.desktop.macos_arm64)
    api(compose.desktop.windows_x64)
    api(compose.desktop.linux_x64)
    api(compose.desktop.linux_arm64)
}

intellijPlugin(group = "org.jetbrains.compose.intellij.platform")
