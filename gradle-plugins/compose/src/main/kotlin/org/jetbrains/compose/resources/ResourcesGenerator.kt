package org.jetbrains.compose.resources

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.internal.KOTLIN_JVM_PLUGIN_ID
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.compose.internal.utils.registerTask
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.ComposeKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfoOrNull
import org.jetbrains.kotlin.gradle.utils.ObservableSet
import java.io.File
import javax.inject.Inject

private const val COMPOSE_RESOURCES_DIR = "composeResources"
private const val RES_GEN_DIR = "generated/compose/resourceGenerator"
private const val KMP_RES_EXT = "multiplatformResourcesPublication"
private val androidPluginIds = listOf(
    "com.android.application",
    "com.android.library"
)

internal fun Project.configureComposeResources() {
    val projectId = provider {
        val groupName = project.group.toString().lowercase().asUnderscoredIdentifier()
        val moduleName = project.name.lowercase().asUnderscoredIdentifier()
        if (groupName.isNotEmpty()) "$groupName.$moduleName"
        else moduleName
    }

    plugins.withId(KOTLIN_MPP_PLUGIN_ID) {
        val kotlinExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

        val hasKmpResources = extraProperties.has(KMP_RES_EXT)
        if (hasKmpResources) {
            configureKmpResources(kotlinExtension, extraProperties.get(KMP_RES_EXT)!!, projectId)
        } else {
            //current KGP doesn't have KPM resources
            configureComposeResources(kotlinExtension, KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME, projectId)

            //when applied AGP then configure android resources
            androidPluginIds.forEach { pluginId ->
                plugins.withId(pluginId) {
                    val androidExtension = project.extensions.getByType(BaseExtension::class.java)
                    configureAndroidComposeResources(kotlinExtension, androidExtension)
                }
            }
        }
    }
    plugins.withId(KOTLIN_JVM_PLUGIN_ID) {
        val kotlinExtension = project.extensions.getByType(KotlinProjectExtension::class.java)
        configureComposeResources(kotlinExtension, SourceSet.MAIN_SOURCE_SET_NAME, projectId)
    }
}

private fun Project.configureComposeResources(
    kotlinExtension: KotlinProjectExtension,
    commonSourceSetName: String,
    projectId: Provider<String>
) {
    logger.lifecycle("Configure compose resources")
    kotlinExtension.sourceSets.all { sourceSet ->
        val sourceSetName = sourceSet.name
        val composeResourcesPath = project.projectDir.resolve("src/$sourceSetName/$COMPOSE_RESOURCES_DIR")

        //To compose resources will be packed to a final artefact we need to mark them as resources
        //sourceSet.resources works for all targets except ANDROID!
        sourceSet.resources.srcDirs(composeResourcesPath)

        if (sourceSetName == commonSourceSetName) {
            configureResourceGenerator(composeResourcesPath, sourceSet, projectId, false)
        }
    }
}

@OptIn(ComposeKotlinGradlePluginApi::class)
private fun Project.configureKmpResources(
    kotlinExtension: KotlinProjectExtension,
    kmpResources: Any,
    projectId: Provider<String>
) {
    kotlinExtension as KotlinMultiplatformExtension
    kmpResources as KotlinTargetResourcesPublication

    logger.lifecycle("Configure KMP resources")
    kotlinExtension.targets
        .matching { target -> kmpResources.canPublishResources(target) }
        .all { target ->
            kmpResources.publishResourcesAsKotlinComponent(
                target,
                { sourceSet ->
                    KotlinTargetResourcesPublication.ResourceRoot(
                        project.provider { project.file("src/${sourceSet.name}/$COMPOSE_RESOURCES_DIR") },
                        emptyList(),
                        //for android target exclude fonts
                        if (target is KotlinAndroidTarget) listOf("**/font*/*") else emptyList()
                    )
                },
                projectId.asModuleDir()
            )

            if (target is KotlinAndroidTarget) {
                //for android target publish fonts in assets
                kmpResources.publishInAndroidAssets(
                    target,
                    { sourceSet ->
                        KotlinTargetResourcesPublication.ResourceRoot(
                            project.provider { project.file("src/${sourceSet.name}/$COMPOSE_RESOURCES_DIR") },
                            listOf("**/font*/*"),
                            emptyList()
                        )
                    },
                    projectId.asModuleDir()
                )
            }
        }
    kotlinExtension.sourceSets.all { sourceSet ->
        val sourceSetName = sourceSet.name
        if (sourceSetName == KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME) {
            val composeResourcesPath = project.projectDir.resolve("src/$sourceSetName/$COMPOSE_RESOURCES_DIR")
            configureResourceGenerator(composeResourcesPath, sourceSet, projectId, true)
        }
    }
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
private fun Project.configureAndroidComposeResources(
    kotlinExtension: KotlinMultiplatformExtension,
    androidExtension: BaseExtension
) {
    //mark all composeResources as Android resources
    kotlinExtension.targets.withType(KotlinAndroidTarget::class.java).all { androidTarget ->
        androidTarget.compilations.all { compilation: KotlinJvmAndroidCompilation ->
            compilation.defaultSourceSet.androidSourceSetInfoOrNull?.let { kotlinAndroidSourceSet ->
                androidExtension.sourceSets
                    .matching { it.name == kotlinAndroidSourceSet.androidSourceSetName }
                    .all { androidSourceSet ->
                        (compilation.allKotlinSourceSets as? ObservableSet<KotlinSourceSet>)?.forAll { kotlinSourceSet ->
                            androidSourceSet.resources.srcDir(
                                projectDir.resolve("src/${kotlinSourceSet.name}/$COMPOSE_RESOURCES_DIR")
                            )
                        }
                    }
            }
        }
    }

    //copy fonts from the compose resources dir to android assets
    val commonResourcesDir = projectDir.resolve(
        "src/${KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME}/$COMPOSE_RESOURCES_DIR"
    )
    val androidComponents = project.extensions.findByType(AndroidComponentsExtension::class.java) ?: return
    androidComponents.onVariants { variant ->
        val copyFonts = registerTask<CopyAndroidFontsToAssetsTask>(
            "copy${variant.name.uppercaseFirstChar()}FontsToAndroidAssets"
        ) {
            from.set(commonResourcesDir)
        }
        variant.sources?.assets?.addGeneratedSourceDirectory(
            taskProvider = copyFonts,
            wiredWith = CopyAndroidFontsToAssetsTask::outputDirectory
        )
        //exclude a duplication of fonts in apks
        variant.packaging.resources.excludes.add("**/font*/*")
    }
}

private fun Project.configureResourceGenerator(
    commonComposeResourcesDir: File,
    commonSourceSet: KotlinSourceSet,
    projectId: Provider<String>,
    generateModulePath: Boolean
) {
    val packageName = projectId.map { "$it.generated.resources" }

    fun buildDir(path: String) = layout.dir(layout.buildDirectory.map { File(it.asFile, path) })

    //lazy check a dependency on the Resources library
    val shouldGenerateResClass: Provider<Boolean> = provider {
        if (ComposeProperties.alwaysGenerateResourceAccessors(project).get()) {
            true
        } else {
            configurations.run {
                //because the implementation configuration doesn't extend the api in the KGP ¯\_(ツ)_/¯
                getByName(commonSourceSet.implementationConfigurationName).allDependencies +
                        getByName(commonSourceSet.apiConfigurationName).allDependencies
            }.any { dep ->
                val depStringNotation = dep.let { "${it.group}:${it.name}:${it.version}" }
                depStringNotation == ComposePlugin.CommonComponentsDependencies.resources
            }
        }
    }

    val genTask = tasks.register(
        "generateComposeResClass",
        GenerateResClassTask::class.java
    ) { task ->
        task.packageName.set(packageName)
        task.shouldGenerateResClass.set(shouldGenerateResClass)
        task.resDir.set(commonComposeResourcesDir)
        task.codeDir.set(buildDir("$RES_GEN_DIR/kotlin"))

        if (generateModulePath) {
            task.moduleDir.set(projectId.asModuleDir())
        }
    }

    //register generated source set
    commonSourceSet.kotlin.srcDir(genTask.map { it.codeDir })

    //setup task execution during IDE import
    tasks.configureEach {
        if (it.name == "prepareKotlinIdeaImport") {
            it.dependsOn(genTask)
        }
    }
}

private fun Provider<String>.asModuleDir() = map { File("$COMPOSE_RESOURCES_DIR/$it") }

//Copy task doesn't work with 'variant.sources?.assets?.addGeneratedSourceDirectory' API
internal abstract class CopyAndroidFontsToAssetsTask : DefaultTask() {
    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @get:Input
    abstract val from: Property<File>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun action() {
        fileSystem.copy {
            it.includeEmptyDirs = false
            it.from(from)
            it.include("**/font*/*")
            it.into(outputDirectory)
        }
    }
}