import org.jetbrains.compose.compose

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij")
    id("org.jetbrains.compose")
}

jvmTarget("11")

dependencies {
    compileOnly(compose.desktop.currentOs)
}

intellijPlugin(group = "org.jetbrains.compose.intellij.platform.sample.plugin1") {
    plugins.set(
        listOf(
            project(":compose-intellij-platform"),
            project(":compose-intellij-platform:sample:base")
        )
    )
}
