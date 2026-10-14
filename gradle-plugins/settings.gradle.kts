pluginManagement {
    repositories {
        mavenCentral {
            url = uri("https://cache-redirector.jetbrains.com/maven-central")
        }
        gradlePluginPortal()
    }
}

include(":compose")
include(":preview-rpc")
include(":jdk-version-probe")
