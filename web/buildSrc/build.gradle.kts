repositories {
    gradlePluginPortal()
    mavenCentral()
    maven { 
        url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") 
    }
    maven {
        url = uri("https://packages.jetbrains.team/maven/p/ui/dev")
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.30"
}
