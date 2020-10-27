import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.compose.desktop.application")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(project(":common"))
}

compose.desktop {
    application {
        mainClass = "example.imageviewer.MainKt"

        nativeExecutables {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ImageViewer"
            modules("jdk.crypto.ec")

            val iconsRoot = project.file("../../../artwork/icons")
            macOS {
                iconFile.set(iconsRoot.resolve("icon-mac.icns"))
            }
            windows {
                iconFile.set(iconsRoot.resolve("icon-windows.ico"))
            }
            linux {
                iconFile.set(iconsRoot.resolve("icon-linux.png"))
            }
        }
    }
}
