import org.gradle.api.Project

open class MavenPublicationConfigExtension {
    lateinit var artifactId: String
    lateinit var displayName: String
    lateinit var description: String
}

val Project.mavenPublicationConfig: MavenPublicationConfigExtension?
    get() = extensions.findByType(MavenPublicationConfigExtension::class.java)

fun Project.mavenPublicationConfig(fn: MavenPublicationConfigExtension.() -> Unit) {
    extensions.create("mavenPublicationConfig", MavenPublicationConfigExtension::class.java).apply(fn)
}