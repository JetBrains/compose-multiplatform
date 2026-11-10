import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

repositories {
    mavenCentral {
        url = uri("https://cache-redirector.jetbrains.com/maven-central")
    }
    maven("https://packages.jetbrains.team/maven/p/cmp/dev")
    google()
}

val composeVersion = providers.gradleProperty("compose.version").get()

dependencies {
    // Root KMP artifacts (not `-desktop` platform ones): resolve via the root `.module`
    // metadata (available-at -> platform modules). This is the dependency style
    // `compose.desktop.currentOs` is deprecated in favor of.
    implementation("org.jetbrains.compose.ui:ui:$composeVersion")
    implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")
    implementation("org.jetbrains.compose.material:material:$composeVersion")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KotlinJvmComposeDesktopJdk11Test"
            packageVersion = "1.0.0"
        }
    }
}
