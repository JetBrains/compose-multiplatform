import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val properties = ComposeUberJarProperties()

repositories {
    mavenCentral()
    maven(properties.composeRepoUrl)
    google()
}

val composeVersion: String by lazy {
    (properties.composeVersionFile?.let {
        project.file(it).readText().trim()
    } ?: properties.composeVersion!!).also {
        project.logger.quiet("Compose version: '$it'")
    }
}

dependencies {
    implementation("org.jetbrains.compose.desktop:desktop-jvm-macos-x64:$composeVersion")
    implementation("org.jetbrains.compose.desktop:desktop-jvm-linux-x64:$composeVersion")
}

val shadowJar = tasks.named("shadowJar", ShadowJar::class) {
    dependencies {
        include { it.moduleGroup.startsWith("org.jetbrains.compose") }
    }
    archiveFileName.set("compose-full.jar")
}

tasks.register("publishToComposeRepo") {
    dependsOn(tasks.named("publishAllPublicationsToComposeRepoRepository"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.jetbrains.compose"
            artifactId = "compose-full"
            version = composeVersion
            artifacts.artifact(shadowJar.map { it.archiveFile.get() })
        }
    }
    repositories {
        maven(properties.composeRepoUrl) {
            name = "ComposeRepo"
            authentication {
                credentials {
                    username = properties.composeRepoUserName
                    password = properties.composeRepoKey
                }
            }
        }
    }
}

class ComposeUberJarProperties {
    val composeVersion: String?
        get() = typedProperty<String>(COMPOSE_VERSION_PROPERTY)

    val composeVersionFile: String?
        get() = typedProperty<String>(COMPOSE_VERSION_FILE_PROPERTY)

    val composeRepoUrl: String
        get() = System.getenv("COMPOSE_REPO_URL") ?: "https://maven.pkg.jetbrains.space/public/p/compose/dev"

    val composeRepoUserName: String?
        get() = System.getenv("COMPOSE_REPO_USERNAME")

    val composeRepoKey: String?
        get() = System.getenv("COMPOSE_REPO_KEY")

    companion object {
        const val COMPOSE_VERSION_PROPERTY = "compose.version"
        const val COMPOSE_VERSION_FILE_PROPERTY = "compose.version.file"

        inline fun <reified T> Project.typedProperty(name: String): T? =
                project.findProperty(name) as? T
    }
}
