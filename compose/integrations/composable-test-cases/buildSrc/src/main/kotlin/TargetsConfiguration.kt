import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

fun KotlinSourceSet.libDependencyForMain() {
    val projectName = this.project.name
    if (!projectName.endsWith("-main")) error("Unexpected main module name: $projectName")
    dependencies {
        implementation(project(":" + projectName.replace("-main", "-lib")))
    }
}
