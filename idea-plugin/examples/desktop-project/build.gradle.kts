import org.jetbrains.compose.compose

plugins {
    // __KOTLIN_COMPOSE_VERSION__
    kotlin("jvm") version "1.4.32"
    id("org.jetbrains.compose") version "0.4.0-preview-annotation-build53"
}

repositories {
    mavenLocal()
    jcenter()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}
