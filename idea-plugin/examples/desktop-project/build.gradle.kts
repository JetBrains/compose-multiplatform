import org.jetbrains.compose.compose

plugins {
    // __KOTLIN_COMPOSE_VERSION__
    kotlin("jvm") version "1.5.10"
    id("org.jetbrains.compose") version "0.0.0-non-interactive-preview-build88"
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.4")
    implementation(compose.uiTooling)
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}
