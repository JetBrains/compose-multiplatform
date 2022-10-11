import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm("desktop")
    sourceSets {
        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(project(":library"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.mylibrary.demo.MainKt"
    }
}
