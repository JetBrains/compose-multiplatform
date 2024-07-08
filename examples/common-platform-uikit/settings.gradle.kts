rootProject.name = "common-platform-uikit"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        mavenLocal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "com.android") useModule("com.android.tools.build:gradle:7.0.3")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

include(":android")
include(":desktop")
include(":web")
include(":common")

