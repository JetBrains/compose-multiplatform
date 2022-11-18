pluginManagement {
    repositories {
        google()
        jcenter()
        gradlePluginPortal()
        mavenCentral()
    }
    
}
rootProject.name = "falling-balls-mpp"

include(":androidApp")
include(":shared")
include(":desktopApp")
include(":jsApp")
