import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm("desktop")
    js(IR) {
        browser()
        binaries.executable()
    }

    sourceSets {
        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)

            }
        }
        named("commonMain") {
            dependencies {
                implementation(project(":Kapp:library"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.jetbrains.compose.kapp.demo.Simple_desktopKt"
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}
