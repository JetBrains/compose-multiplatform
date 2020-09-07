import org.jetbrains.compose.compose

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    java
    application
}

dependencies {
    implementation(compose.desktop.all)
    implementation(project(":common"))
}

application {
    mainClassName = "androidx.ui.examples.jetissues.MainKt"
}