package org.jetbrains.compose.resources

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import java.io.File

abstract class ResourcesExtension {
    /**
     * Whether the generated resources accessors class should be public or not.
     *
     * Default is false.
     */
    var publicResClass: Boolean = false

    /**
     * The unique identifier of the resources in the current project.
     * Uses as package for the generated Res class and for isolation resources in a final artefact.
     *
     * If it is empty then `{group name}.{module name}.generated.resources` will be used.
     *
     */
    var packageOfResClass: String = ""

    enum class ResourceClassGeneration { Auto, Always, Never }

    //to support groovy DSL
    val auto = ResourceClassGeneration.Auto
    val always = ResourceClassGeneration.Always
    val never = ResourceClassGeneration.Never

    /**
     * The mode of resource class generation.
     *
     * - `auto`: The Res class will be generated if the current project has an explicit "implementation" or "api" dependency on the resource's library.
     * - `always`: Unconditionally generate the Res class. This may be useful when the resources library is available transitively.
     * - `never`: Never generate the Res class.
     */
    var generateResClass: ResourceClassGeneration = auto

    internal val customResourceDirectories: MutableMap<String, Provider<Directory>> = mutableMapOf()

    /**
     * Associates a custom resource directory with a specific source set.
     *
     * @param sourceSetName the name of the source set to associate the custom resource directory with
     * @param directoryProvider the provider that provides the custom directory
     */
    fun customDirectory(sourceSetName: String, directoryProvider: Provider<Directory>) {
        customResourceDirectories[sourceSetName] = directoryProvider
    }
}

internal fun Provider<ResourcesExtension>.getResourcePackage(project: Project) = map { config ->
    config.packageOfResClass.takeIf { it.isNotEmpty() } ?: run {
        val groupName = project.group.toString().lowercase().asUnderscoredIdentifier()
        val moduleName = project.name.lowercase().asUnderscoredIdentifier()
        val id = if (groupName.isNotEmpty()) "$groupName.$moduleName" else moduleName
        "$id.generated.resources"
    }
}
//the dir where resources must be placed in the final artefact
internal fun Provider<ResourcesExtension>.getModuleResourcesDir(project: Project) =
    getResourcePackage(project).map { packageName -> File("$COMPOSE_RESOURCES_DIR/$packageName") }
