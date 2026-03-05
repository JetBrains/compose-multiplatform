plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()
    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
            }
        }
        jvmMain.dependencies {
            implementation(libs.compose.ui)
            implementation(project(":AnimatedImage:library"))
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.jetbrains.compose.animatedimage.demo.MainKt"
    }
}