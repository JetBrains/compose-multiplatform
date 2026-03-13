import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

repositories {
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/cmp/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    implementation("org.jetbrains.compose.desktop:desktop:${project.extra["compose.version"]}")
    implementation("org.jetbrains.compose.material:material:${project.extra["compose.version"]}")

    // Include the Test API
    // compileOnly instead of testImplementation for checking compilation of the tutorials
    compileOnly("org.jetbrains.compose.ui:ui-test-junit4:${project.extra["compose.version"]}")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KotlinJvmComposeDesktopApplication"
            packageVersion = "1.0.0"
        }
    }
}
