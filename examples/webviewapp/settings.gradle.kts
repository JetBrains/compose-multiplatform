pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")

    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven("https://jitpack.io")
        maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
    }
}

include(
    ":webview",
    ":android",
    ":desktop"
)
