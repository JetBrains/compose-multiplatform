import org.gradle.api.Project

// "Global" properties
object BuildProperties {
    const val name = "JetBrains Compose Plugin"
    const val group = "org.jetbrains.compose"
    const val website = "https://jetbrains.org/compose"
    const val vcs = "https://github.com/JetBrains/compose-jb"
    fun composeVersion(project: Project): String =
        System.getenv("COMPOSE_GRADLE_PLUGIN_COMPOSE_VERSION")
            ?: project.findProperty("compose.version") as String
    fun deployVersion(project: Project): String =
        System.getenv("COMPOSE_GRADLE_PLUGIN_VERSION")
            ?: project.findProperty("deploy.version") as String
}
