plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral {
        url = uri("https://cache-redirector.jetbrains.com/maven-central")
    }
    gradlePluginPortal()
}

dependencies {
    compileOnly(gradleApi())
}
