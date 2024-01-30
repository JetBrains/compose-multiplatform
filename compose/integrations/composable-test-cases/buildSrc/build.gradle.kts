import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/")
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


tasks.withType<KotlinCompile<*>>().configureEach {
    kotlinOptions {
        languageVersion = "1.9"
        apiVersion = "1.9"
    }
}