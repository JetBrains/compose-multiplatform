plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
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
            implementation(compose.desktop.currentOs)
            implementation(project(":SplitPane:library"))
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.jetbrains.compose.splitpane.demo.MainKt"
    }
}