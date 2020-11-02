import org.jetbrains.compose.compose

plugins {
    kotlin("jvm") version "1.4.0"
    id("org.jetbrains.compose") version (System.getenv("COMPOSE_TEMPLATE_COMPOSE_VERSION") ?: "0.1.0-m1-build57")
    application
}

repositories {
    jcenter()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
}

application {
    mainClassName = "MainKt"
}
