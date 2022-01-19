import org.jetbrains.compose.compose

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij")
    id("org.jetbrains.compose") version "1.0.1"
}

jvmTarget("11")

val skikoVersion = project.property("skiko.version").toString()
dependencies {
    // todo unpack skiko native libs
    fun skikoDependency(artifact: String) = api("org.jetbrains.skiko:$artifact:$skikoVersion")

    skikoDependency("skiko-awt")
    skikoDependency("skiko-awt-runtime-linux-x64")
    skikoDependency("skiko-awt-runtime-windows-x64")
    skikoDependency("skiko-awt-runtime-macos-x64")
    skikoDependency("skiko-awt-runtime-macos-arm64")

    api(compose.desktop.currentOs) {
        exclude(group = "org.jetbrains.skiko")
    }
}

intellijPlugin(group = "org.jetbrains.compose.intellij.platform")
