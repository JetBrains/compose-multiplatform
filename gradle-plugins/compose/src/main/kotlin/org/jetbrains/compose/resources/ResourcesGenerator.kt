package org.jetbrains.compose.resources

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.tasks.ProcessJavaResTask
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
import org.jetbrains.compose.internal.utils.*
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfoOrNull
import org.jetbrains.kotlin.gradle.utils.ObservableSet
import java.io.File
import javax.inject.Inject

internal const val COMPOSE_RESOURCES_DIR = "composeResources"
internal const val RES_GEN_DIR = "generated/compose/resourceGenerator"
private val androidPluginIds = listOf(
    "com.android.application",
    "com.android.library"
)

internal fun Project.configureComposeResources() {
    plugins.withId(KOTLIN_MPP_PLUGIN_ID) {
        val kotlinExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        configureComposeResources(kotlinExtension, KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME)

        //when applied AGP then configure android resources
        androidPluginIds.forEach { pluginId ->
            plugins.withId(pluginId) {
                val androidExtension = project.extensions.getByType(BaseExtension::class.java)
                configureAndroidComposeResources(kotlinExtension, androidExtension)
            }
        }
    }
    plugins.withId(KOTLIN_JVM_PLUGIN_ID) {
        val kotlinExtension = project.extensions.getByType(KotlinProjectExtension::class.java)
        configureComposeResources(kotlinExtension, SourceSet.MAIN_SOURCE_SET_NAME)
    }
}

private fun Project.configureComposeResources(kotlinExtension: KotlinProjectExtension, commonSourceSetName: String) {
    kotlinExtension.sourceSets.all { sourceSet ->
        val sourceSetName = sourceSet.name
        val composeResourcesPath = project.projectDir.resolve("src/$sourceSetName/$COMPOSE_RESOURCES_DIR")

        //To compose resources will be packed to a final artefact we need to mark them as resources
        //sourceSet.resources works for all targets except ANDROID!
        sourceSet.resources.srcDirs(composeResourcesPath)

        if (sourceSetName == commonSourceSetName) {
            configureResourceGenerator(composeResourcesPath, sourceSet)
        }
    }
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
private fun Project.configureAndroidComposeResources(
    kotlinExtension: KotlinMultiplatformExtension,
    androidExtension: BaseExtension
) {
    val commonResourcesDir = projectDir.resolve("src/${KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME}/$COMPOSE_RESOURCES_DIR")

    //Copy common compose resources except fonts to android resources
    val commonAndroidComposeResourcesDir = layout.buildDirectory.dir("$RES_GEN_DIR/commonAndroidComposeResources")
    val copyCommonAndroidComposeResources = registerTask<Copy>(
        "copyCommonAndroidComposeResources"
    ) {
        includeEmptyDirs = false
        from(commonResourcesDir)
        exclude("**/font*/*")
        into(commonAndroidComposeResourcesDir)
    }

    //mark all composeResources as Android resources
    kotlinExtension.targets.withType(KotlinAndroidTarget::class.java).all { androidTarget ->
        androidTarget.compilations.all { compilation: KotlinJvmAndroidCompilation ->
            compilation.defaultSourceSet.androidSourceSetInfoOrNull?.let { kotlinAndroidSourceSet ->
                androidExtension.sourceSets
                    .matching { it.name == kotlinAndroidSourceSet.androidSourceSetName }
                    .all { androidSourceSet ->
                        compilation.androidVariant.processJavaResourcesProvider.dependsOn(copyCommonAndroidComposeResources)
                        androidSourceSet.resources.srcDir(commonAndroidComposeResourcesDir)
                        (compilation.allKotlinSourceSets as? ObservableSet<KotlinSourceSet>)?.forAll { kotlinSourceSet ->
                            if (kotlinSourceSet.name != KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME) {
                                androidSourceSet.resources.srcDir(
                                    projectDir.resolve("src/${kotlinSourceSet.name}/$COMPOSE_RESOURCES_DIR")
                                )
                            }
                        }
                    }
            }
        }
    }

    //copy fonts from the compose resources dir to android assets
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
    }
}

private fun Project.configureResourceGenerator(commonComposeResourcesDir: File, commonSourceSet: KotlinSourceSet) {
    val packageName = provider {
        buildString {
            val group = project.group.toString().lowercase().asUnderscoredIdentifier()
            append(group)
            if (group.isNotEmpty()) append(".")
            append(project.name.lowercase())
            append(".generated.resources")
        }
    }

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
    ) {
        it.packageName.set(packageName)
        it.shouldGenerateResClass.set(shouldGenerateResClass)
        it.resDir.set(commonComposeResourcesDir)
        it.codeDir.set(buildDir("$RES_GEN_DIR/kotlin"))
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