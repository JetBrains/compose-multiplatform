pluginManagement {
    repositories {
        mavenCentral {
            url = uri("https://cache-redirector.jetbrains.com/maven-central")
        }
        gradlePluginPortal()
    }
}

include(":publishing")