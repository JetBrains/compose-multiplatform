import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    java
    application
}

kotlin {
    jvm {
        withJava()
    }

    sourceSets {
        named("jvmMain") {
            dependencies {
                implementation(compose.desktop.all)
                implementation(project(":common"))
            }
        }
    }
}

application {
    mainClassName = "org.jetbrains.codeviewer.MainKt"
}