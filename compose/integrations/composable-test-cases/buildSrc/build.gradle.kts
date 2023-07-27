import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

repositories {
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    kotlin("jvm")
}

kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_1_9)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
}
