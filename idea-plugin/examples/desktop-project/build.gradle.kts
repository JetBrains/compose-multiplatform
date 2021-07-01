import org.jetbrains.compose.compose

plugins {
    // __KOTLIN_COMPOSE_VERSION__
    kotlin("jvm") version "1.5.10"
    // __LATEST_COMPOSE_RELEASE_VERSION__
    id("org.jetbrains.compose") version "0.5.0-build229"
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.uiTooling)
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}
