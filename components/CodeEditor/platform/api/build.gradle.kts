plugins {
    id("java-library")
    id("maven-publish")
}

configureMavenPublication(
    groupId = "org.jetbrains.compose.components",
    artifactId = "components-codeeditor-platform-api",
    name = "CodeEditor Platform API for Compose JB"
)
