repositories {
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    kotlin("jvm")
}


java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
}
