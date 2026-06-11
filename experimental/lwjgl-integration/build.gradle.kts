import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10"
    id("org.jetbrains.compose") version "1.8.0-alpha02"
}

repositories {
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/cmp/dev")
    google()
}

val osName: String = System.getProperty("os.name")
val os = when {
    osName == "Mac OS X" -> "macos"
    osName == "Linux" -> "linux"
    osName.startsWith("Win") -> "windows"
    else -> throw Error("Unknown OS $osName")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.lwjgl:lwjgl:3.2.3")
    implementation("org.lwjgl:lwjgl-glfw:3.2.3")
    implementation("org.lwjgl:lwjgl-opengl:3.2.3")
    implementation("org.lwjgl:lwjgl:3.2.3:natives-$os")
    implementation("org.lwjgl:lwjgl-glfw:3.2.3:natives-$os")
    implementation("org.lwjgl:lwjgl-opengl:3.2.3:natives-$os")
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
