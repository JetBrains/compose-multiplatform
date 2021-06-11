import org.jetbrains.compose.compose

plugins {
    // __KOTLIN_COMPOSE_VERSION__
    kotlin("jvm") version "1.5.0"
    id("org.jetbrains.compose") version "0.4.0-preview-remote"
}

repositories {
    mavenCentral()
    mavenLocal()

    maven("/Users/alexey.tsvetkov/projects/androidx/out/androidx/build/support_repo")
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
