plugins {
    kotlin("jvm")
    id("maven-publish")
}

mavenPublicationConfig {
    displayName = "Compose Desktop Preview Runtime"
    description = "Runtime helpers for Compose Desktop Preview"
    artifactId = "compose-preview-runtime-desktop"
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib")
}