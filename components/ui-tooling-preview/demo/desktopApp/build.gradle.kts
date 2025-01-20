plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()
    sourceSets {
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(project(":ui-tooling-preview:demo:shared"))
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}
