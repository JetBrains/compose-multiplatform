import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.4.32"
    id("org.jetbrains.intellij") version "0.7.2"
    id("org.jetbrains.changelog") version "1.1.2"
}

fun properties(key: String) = project.findProperty(key).toString()

group = "org.jetbrains.compose.desktop.ide"
version = properties("deploy.version")

repositories {
    mavenCentral()
    jcenter()
}

intellij {
    pluginName = "Compose for Desktop IDE Support"
    type = "IC"
    downloadSources = true
    updateSinceUntilBuild = true
    version = "203.7717.56"

    setPlugins(
        "java",
        "com.intellij.gradle",
        "org.jetbrains.kotlin:203-1.4.32-release-IJ7148.5"
    )
}

tasks.buildSearchableOptions {
    // temporary workaround
    enabled = false
}

tasks {
    // Set the compatibility versions to 1.8
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    withType<KotlinJvmCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    publishPlugin {
        token(System.getenv("IDE_PLUGIN_PUBLISH_TOKEN"))
        channels("Alpha")
    }
}
