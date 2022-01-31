import org.jetbrains.compose.compose

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij")
}

jvmTarget("11")

intellijPlugin(group = "org.jetbrains.compose.intellij.platform.sample.all.plugins") {
    plugins.set(
        listOf(
            project(":compose-intellij-platform"),
            project(":compose-intellij-platform:sample:base"),
            project(":compose-intellij-platform:sample:plugin-1"),
            project(":compose-intellij-platform:sample:plugin-2"),
        )
    )
}
