import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("java")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.plugin)
    alias(libs.plugins.intellij.changelog)
}

val projectProperties = ProjectProperties(project)

group = "org.jetbrains.compose.desktop.ide"

version = projectProperties.deployVersion

repositories {
    mavenCentral()

    intellijPlatform { defaultRepositories() }
}

dependencies {
    implementation("org.jetbrains.compose:preview-rpc")

    intellijPlatform {
        intellijIdeaCommunity(libs.versions.idea)
        instrumentationTools()

        bundledPlugins("com.intellij.java", "org.jetbrains.kotlin", "com.intellij.gradle")
    }
}

intellijPlatform {
    pluginConfiguration { name = "Compose Multiplatform IDE Support" }
    buildSearchableOptions = false
    autoReload = false

    publishing {
        token = System.getenv("IDE_PLUGIN_PUBLISH_TOKEN")
        channels = projectProperties.pluginChannels
    }

    pluginVerification { ides { recommended() } }
}

tasks {
    // Set the compatibility versions to 1.8
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
    withType<KotlinJvmCompile> { compilerOptions.jvmTarget.set(JvmTarget.JVM_11) }
}

class ProjectProperties(private val project: Project) {
    val deployVersion
        get() = stringProperty("deploy.version")

    val pluginChannels
        get() = listProperty("plugin.channels")

    private fun stringProperty(key: String): String = project.findProperty(key)!!.toString()

    private fun listProperty(key: String): List<String> =
        stringProperty(key).split(",").map { it.trim() }
}
