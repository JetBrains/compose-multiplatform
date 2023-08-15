/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal.resources

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.jetbrains.compose.experimental.uikit.internal.utils.IosGradleProperties
import org.jetbrains.compose.experimental.uikit.internal.utils.asIosNativeTargetOrNull
import org.jetbrains.compose.experimental.uikit.internal.utils.cocoapodsExt
import org.jetbrains.compose.experimental.uikit.internal.utils.withCocoapodsPlugin
import org.jetbrains.compose.experimental.uikit.tasks.SyncComposeResourcesForIosTask
import org.jetbrains.compose.internal.utils.joinLowerCamelCase
import org.jetbrains.compose.internal.utils.new
import org.jetbrains.compose.internal.utils.registerOrConfigure
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import java.io.File

private val incompatiblePlugins = listOf(
    "dev.icerock.mobile.multiplatform-resources",
    "io.github.skeptick.libres",
)

internal fun Project.configureSyncTask(mppExt: KotlinMultiplatformExtension) {
    fun reportSyncIsDisabled(reason: String) {
        logger.info("Compose Multiplatform resource management for iOS is disabled: $reason")
    }

    if (!IosGradleProperties.syncResources(providers).get()) {
        reportSyncIsDisabled("'${IosGradleProperties.SYNC_RESOURCES_PROPERTY}' value is 'false'")
        return
    }

    for (incompatiblePluginId in incompatiblePlugins) {
        if (project.plugins.hasPlugin(incompatiblePluginId)) {
            reportSyncIsDisabled("resource management is not compatible with '$incompatiblePluginId'")
            return
        }
    }

    with (SyncIosResourcesContext(project, mppExt)) {
        configureSyncResourcesTasks()
        configureCocoapodsResourcesAttribute()
    }
}

private class SyncIosResourcesContext(
    val project: Project,
    val mppExt: KotlinMultiplatformExtension
) {
    fun syncDirFor(framework: Framework): Provider<Directory> {
        val providers = framework.project.providers
        return if (framework.isCocoapodsFramework) {
            project.layout.buildDirectory.dir("compose/ios/${framework.baseName}/compose-resources/")
        } else {
            providers.environmentVariable("BUILT_PRODUCTS_DIR")
                .zip(providers.environmentVariable("CONTENTS_FOLDER_PATH")) { builtProductsDir, contentsFolderPath ->
                    File(builtProductsDir)
                        .resolve(contentsFolderPath)
                        .resolve("compose-resources")
                        .canonicalPath
                }.flatMap {
                    framework.project.objects.directoryProperty().apply { set(File(it)) }
                }
        }
    }


    fun configureEachIosFramework(fn: (Framework) -> Unit) {
        mppExt.targets.all { target ->
            target.asIosNativeTargetOrNull()?.let { iosTarget ->
                iosTarget.binaries.withType(Framework::class.java).configureEach { framework ->
                    fn(framework)
                }
            }
        }
    }
}

private const val RESOURCES_SPEC_ATTR = "resources"
private fun SyncIosResourcesContext.configureCocoapodsResourcesAttribute() {
    project.withCocoapodsPlugin {
        project.gradle.taskGraph.whenReady {
            val cocoapodsExt = mppExt.cocoapodsExt
            val specAttributes = cocoapodsExt.extraSpecAttributes
            val resourcesSpec = specAttributes[RESOURCES_SPEC_ATTR]
            if (!resourcesSpec.isNullOrBlank()) {
                error("""
                    |Kotlin.cocoapods.extraSpecAttributes["resources"] is not compatible with Compose Multiplatform's resources management for iOS.
                    |  * Recommended action: remove extraSpecAttributes["resources"] from '${project.buildFile}' and run '${project.path}:podInstall' once;
                    |  * Alternative action: turn off Compose Multiplatform's resources management for iOS by adding '${IosGradleProperties.SYNC_RESOURCES_PROPERTY}=false' to your gradle.properties;
                """.trimMargin())
            }
            cocoapodsExt.framework {
                val syncDir = syncDirFor(this).get().asFile
                specAttributes[RESOURCES_SPEC_ATTR] = "['${syncDir.relativeTo(project.projectDir).path}']"
                project.tasks.named("podInstall").configure {
                    it.doFirst {
                        syncDir.mkdirs()
                    }
                }
            }
        }
    }
}

private fun SyncIosResourcesContext.configureSyncResourcesTasks() {
    val lazyTasksDependencies = LazyTasksDependencyConfigurator(project.tasks)
    configureEachIosFramework { framework ->
        val frameworkClassifier = framework.namePrefix.uppercaseFirstChar()
        val syncResourcesTaskName = "sync${frameworkClassifier}ComposeResourcesForIos"
        val syncTask = framework.project.tasks.registerOrConfigure<SyncComposeResourcesForIosTask>(syncResourcesTaskName) {
            outputDir.set(syncDirFor(framework))
            iosTargets.add(iosTargetResourcesProvider(framework))
        }
        with (lazyTasksDependencies) {
            if (framework.isCocoapodsFramework) {
                "syncFramework".lazyDependsOn(syncTask.name)
            } else {
                "embedAndSign${frameworkClassifier}AppleFrameworkForXcode".lazyDependsOn(syncTask.name)
            }
        }
    }
}

private val Framework.isCocoapodsFramework: Boolean
    get() = name.startsWith("pod")

private val Framework.namePrefix: String
    get() = extractPrefixFromBinaryName(
        name,
        buildType,
        outputKind.taskNameClassifier
    )

private fun extractPrefixFromBinaryName(name: String, buildType: NativeBuildType, outputKindClassifier: String): String {
    val suffix = joinLowerCamelCase(buildType.getName(), outputKindClassifier)
    return if (name == suffix)
        ""
    else
        name.substringBeforeLast(suffix.uppercaseFirstChar())
}

private fun iosTargetResourcesProvider(framework: Framework): Provider<IosTargetResources> {
    val kotlinTarget = framework.target
    val project = framework.project
    return project.provider {
        val resourceDirs = framework.compilation.allKotlinSourceSets
            .flatMap { sourceSet ->
                sourceSet.resources.srcDirs.map { it.canonicalPath }
            }
        project.objects.new<IosTargetResources>().apply {
            name.set(kotlinTarget.name)
            konanTarget.set(kotlinTarget.konanTarget.name)
            dirs.set(resourceDirs)
        }
    }
}

/**
 * Ensures, that a dependency between tasks is set up,
 * when a dependent task (fromTask) is created, while avoiding eager configuration.
 */
private class LazyTasksDependencyConfigurator(private val tasks: TaskContainer) {
    private val existingDependencies = HashSet<Pair<String, String>>()
    private val requestedDependencies = HashMap<String, MutableSet<String>>()

    init {
        tasks.configureEach { fromTask ->
            val onTasks = requestedDependencies.remove(fromTask.name) ?: return@configureEach
            for (onTaskName in onTasks) {
                val dependency = fromTask.name to onTaskName
                if (existingDependencies.add(dependency)) {
                    fromTask.dependsOn(onTaskName)
                }
            }
        }
    }

    fun String.lazyDependsOn(dependencyTask: String) {
        val dependingTask = this
        val dependency = dependingTask to dependencyTask
        if (dependency in existingDependencies) return

        if (dependingTask in tasks.names) {
            tasks.named(dependingTask).configure { it.dependsOn(dependencyTask) }
            existingDependencies.add(dependency)
        } else {
            requestedDependencies
                .getOrPut(dependingTask) { HashSet() }
                .add(dependencyTask)
        }
    }
}
