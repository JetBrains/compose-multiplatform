import org.jetbrains.compose.compose

plugins {
    kotlin("jvm") version "1.4.0"
    id("org.jetbrains.compose") version "0.1.0-demo3"
    application
}

repositories {
    jcenter()
    maven("https://packages.jetbrains.team/maven/p/ui/dev")
}

dependencies {
    implementation(compose.desktop.all)
}

application {
    mainClassName = "MainKt"
}