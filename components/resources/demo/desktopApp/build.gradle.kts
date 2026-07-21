plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()
    sourceSets {
        jvmMain.dependencies {
            implementation(libs.compose.desktop.jvm)
            implementation(project(":resources:demo:shared"))
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}
