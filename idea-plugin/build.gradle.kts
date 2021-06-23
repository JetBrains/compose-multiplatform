import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.5.10"
    id("org.jetbrains.intellij") version "0.7.2"
    id("org.jetbrains.changelog") version "1.1.2"
}

fun properties(key: String) = project.findProperty(key).toString()

group = "org.jetbrains.compose.desktop.ide"
version = properties("deploy.version")

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.compose:preview-rpc")
}

intellij {
    pluginName = "Compose Multiplatform IDE Support"
    type = properties("platform.type")
    version = properties("platform.version")
    downloadSources = properties("platform.download.sources").toBoolean()

    setPlugins(
        "java",
        "com.intellij.gradle",
        "org.jetbrains.kotlin"
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
        channels(properties("plugin.channels"))
    }

    patchPluginXml {
        sinceBuild(properties("plugin.since.build"))
        untilBuild(properties("plugin.until.build"))
    }

    runPluginVerifier {
        ideVersions(properties("plugin.verifier.ide.versions"))
        downloadDirectory("${project.buildDir}/pluginVerifier/ides")
    }
}
