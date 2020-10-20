import org.gradle.api.Project

// Plugin-specific properties (also see gradle-plugins/build.gradle.kts)
open class GradlePluginConfigExtension {
    lateinit var pluginId: String
    lateinit var artifactId: String
    lateinit var implementationClass: String
    lateinit var displayName: String
    lateinit var description: String
}

val Project.gradlePluginConfig: GradlePluginConfigExtension?
    get() = extensions.findByType(GradlePluginConfigExtension::class.java)

fun Project.gradlePluginConfig(fn: GradlePluginConfigExtension.() -> Unit) {
    extensions.create("gradlePluginConfig", GradlePluginConfigExtension::class.java).apply(fn)
}
