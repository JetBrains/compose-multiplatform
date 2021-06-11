plugins {
    kotlin("jvm")
    id("maven-publish")
}

mavenPublicationConfig {
    displayName = "Compose Desktop Preview Server"
    description = "Compose Desktop Preview Server"
    artifactId = "compose-desktop-preview-server"
}

dependencies {
    implementation(kotlin("stdlib"))
}