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
        mainClass = "org.jetbrains.compose.kapp.demo.MainKt"
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}
