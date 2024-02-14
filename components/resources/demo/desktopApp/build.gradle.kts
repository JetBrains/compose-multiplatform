plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm()
    sourceSets {
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(project(":resources:demo:shared"))
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}
