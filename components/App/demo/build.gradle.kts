import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm("desktop") {}
    sourceSets {
        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(project(":App:library"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.jetbrains.compose.app.demo.MainKt"
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}
