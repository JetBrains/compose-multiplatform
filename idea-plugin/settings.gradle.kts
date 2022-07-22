pluginManagement {
    repositories {
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        gradlePluginPortal()
    }
}

includeBuild("../gradle-plugins") {
    name = "compose-gradle-components"
}