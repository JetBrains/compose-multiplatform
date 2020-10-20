// "Global" properties
object BuildProperties {
    const val name = "Jetpack Compose Plugin"
    const val group = "org.jetbrains.compose"
    const val website = "https://jetbrains.org/compose"
    const val vcs = "https://github.com/JetBrains/compose-jb"
    val composeVersion: String
        get() = System.getenv("COMPOSE_GRADLE_PLUGIN_COMPOSE_VERSION") ?: "0.1.0-SNAPSHOT"
    val version: String
        get() = System.getenv("COMPOSE_GRADLE_PLUGIN_VERSION") ?: composeVersion
}
