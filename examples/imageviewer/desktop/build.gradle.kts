import org.jetbrains.compose.compose

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    java
    application
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(project(":common"))
}

application {
    mainClassName = "example.imageviewer.MainKt"
}