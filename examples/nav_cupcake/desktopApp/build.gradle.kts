import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(projects.shared)
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.jetbrains.nav_cupcake"
            packageVersion = "1.0.0"
        }

        buildTypes.release.proguard {
            configurationFiles.from(project.file("rules.pro"))
        }
    }
}
