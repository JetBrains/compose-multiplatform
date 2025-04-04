import org.jetbrains.compose.*
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            packageVersion = "1.0.0"

            val resourcesRoot = project.layout.projectDirectory.dir("resources")
            macOS {
                iconFile = resourcesRoot.file("icons/macos.icns")
                entitlementsFile = resourcesRoot.file("entitlements.plist")
                runtimeEntitlementsFile = resourcesRoot.file("entitlements.plist")
            }

            windows {
                iconFile = resourcesRoot.file("icons/windows.ico")
            }

            linux {
                iconFile = resourcesRoot.file("icons/linux.png")
            }
        }
    }
}
