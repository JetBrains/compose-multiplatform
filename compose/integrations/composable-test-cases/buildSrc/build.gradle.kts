repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/")
}

plugins {
    `kotlin-dsl`
//    kotlin("jvm") version "2.1.0-RC"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0-RC")
}
