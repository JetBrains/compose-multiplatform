import org.jetbrains.compose.compose

plugins {
    // __KOTLIN_COMPOSE_VERSION__
    kotlin("jvm") version "1.4.32"
    id("org.jetbrains.compose") version "0.4.0-new-preview"
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.4")
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.ui:ui-tooling:0.4.0-new-preview")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}
