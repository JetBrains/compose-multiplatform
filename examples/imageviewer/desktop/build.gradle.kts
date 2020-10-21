import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") // kotlin("jvm") doesn't work well in IDEA/AndroidStudio (https://github.com/JetBrains/compose-jb/issues/22)
    id("org.jetbrains.compose")
    id("org.jetbrains.compose.desktop.application")
}

kotlin {
    jvm {
        withJava()
    }
    sourceSets {
        named("jvmMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(project(":common"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "example.imageviewer.MainKt"

        nativeExecutables {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ImageViewer"
        }
    }
}
