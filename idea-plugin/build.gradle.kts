import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("java")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.sdk)
    alias(libs.plugins.intellij.changelog)
}

val projectProperties = ProjectProperties(project)

group = "org.jetbrains.compose.desktop.ide"
version = projectProperties.deployVersion

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.compose:preview-rpc")
}

intellij {
    pluginName.set("Compose Multiplatform IDE Support")
    type.set(projectProperties.platformType)
    version.set(projectProperties.platformVersion)
    downloadSources.set(projectProperties.platformDownloadSources)
    updateSinceUntilBuild.set(false)

    plugins.set(
        listOf(
            "java",
            "com.intellij.gradle",
            "org.jetbrains.kotlin"
        )
    )
}

tasks.buildSearchableOptions {
    // temporary workaround
    enabled = false
}

tasks {
    // Set the compatibility versions to 1.8
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
    withType<KotlinJvmCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    publishPlugin {
        token.set(System.getenv("IDE_PLUGIN_PUBLISH_TOKEN"))
        channels.set(projectProperties.pluginChannels)
    }

    runPluginVerifier {
        ideVersions.set(projectProperties.pluginVerifierIdeVersions)
    }
}

class ProjectProperties(private val project: Project) {
    val deployVersion get() = stringProperty("deploy.version")
    val platformType get() = stringProperty("platform.type")
    val platformVersion get() = stringProperty("platform.version")
    val platformDownloadSources get() = stringProperty("platform.download.sources").toBoolean()
    val pluginChannels get() = listProperty("plugin.channels")
    val pluginVerifierIdeVersions get() = listProperty("plugin.verifier.ide.versions")

    private fun stringProperty(key: String): String =
        project.findProperty(key)!!.toString()
    private fun listProperty(key: String): List<String> =
        stringProperty(key).split(",").map { it.trim() }
}
