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
        pluginVerifier()

        bundledPlugins("com.intellij.java", "org.jetbrains.kotlin", "com.intellij.gradle")
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "Compose Multiplatform IDE Support"
        ideaVersion {
            sinceBuild = "242.20224"
            untilBuild = provider { null }
        }
    }
    buildSearchableOptions = false
    autoReload = false

    publishing {
        token = System.getenv("IDE_PLUGIN_PUBLISH_TOKEN")
        channels = projectProperties.pluginChannels
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<KotlinJvmCompile> { compilerOptions.jvmTarget.set(JvmTarget.JVM_21) }

    runIde {
        systemProperty("idea.is.internal", true)
        systemProperty("idea.kotlin.plugin.use.k2", true)
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005")
        }
    }
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
