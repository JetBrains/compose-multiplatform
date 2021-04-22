pluginManagement {
    buildscript {
        repositories {
            maven("https://maven.pkg.jetbrains.space/public/p/space/maven")
            mavenCentral()
            gradlePluginPortal()
        }
        dependencies {
            classpath("io.ktor:ktor-client-jetty:1.5.0")
            classpath("org.jetbrains:space-sdk-jvm:68349-beta")
            classpath("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
            classpath("commons-io:commons-io:2.8.0")
        }
    }
}

rootProject.name = "space-delete-package"
