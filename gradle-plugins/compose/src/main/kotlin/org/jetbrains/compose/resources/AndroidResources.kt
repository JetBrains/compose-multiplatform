package org.jetbrains.compose.resources

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Component
import com.android.build.api.variant.HasAndroidTest
import com.android.build.gradle.internal.lint.AndroidLintAnalysisTask
import com.android.build.gradle.internal.lint.LintModelWriterTask
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.compose.internal.utils.registerTask
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import java.io.File
import javax.inject.Inject

internal fun Project.configureAndroidComposeResources(moduleResourceDir: Provider<File>? = null) {
    //copy all compose resources to android assets
    val androidComponents = project.extensions.findByType(AndroidComponentsExtension::class.java) ?: return
    androidComponents.onVariants { variant ->
        configureGeneratedAndroidComponentAssets(variant, moduleResourceDir)

        if (variant is HasAndroidTest) {
            variant.androidTest?.let { androidTest ->
                configureGeneratedAndroidComponentAssets(androidTest, moduleResourceDir)
            }
        }
    }
}

private fun Project.configureGeneratedAndroidComponentAssets(
    component: Component,
    moduleResourceDir: Provider<File>?
) {
    val kotlinExtension = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return
    val camelComponentName = component.name.uppercaseFirstChar()
    val componentAssets = getAndroidComponentComposeResources(kotlinExtension, component.name)
    logger.info("Configure ${component.name} resources for 'android' target")

    val copyComponentAssets = registerTask<CopyResourcesToAndroidAssetsTask>(
        "copy${camelComponentName}ComposeResourcesToAndroidAssets"
    ) {
        from.set(componentAssets)
        moduleResourceDir?.let { relativeResourcePlacement.set(it) }
    }

    component.sources.assets?.addGeneratedSourceDirectory(
        copyComponentAssets,
        CopyResourcesToAndroidAssetsTask::outputDirectory
    )
    tasks.configureEach { task ->
        //fix agp task dependencies for AndroidStudio preview
        if (task.name == "compile${camelComponentName}Sources") {
            task.dependsOn(copyComponentAssets)
        }
        //fix linter task dependencies for `build` task
        if (task is AndroidLintAnalysisTask || task is LintModelWriterTask) {
            task.mustRunAfter(copyComponentAssets)
        }
    }
}

private fun Project.getAndroidComponentComposeResources(
    kotlinExtension: KotlinMultiplatformExtension,
    componentName: String
): FileCollection = project.files({
    kotlinExtension.targets.withType(KotlinAndroidTarget::class.java).flatMap { androidTarget ->
        androidTarget.compilations.flatMap { compilation ->
            if (compilation.androidVariant.name == componentName) {
                compilation.allKotlinSourceSets.map { kotlinSourceSet ->
                    getPreparedComposeResourcesDir(kotlinSourceSet)
                }
            } else emptyList()
        }
    }
})

//Copy task doesn't work with 'variant.sources?.assets?.addGeneratedSourceDirectory' API
internal abstract class CopyResourcesToAndroidAssetsTask : DefaultTask() {
    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    abstract val from: Property<FileCollection>

    @get:Input
    @get:Optional
    abstract val relativeResourcePlacement: Property<File>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun action() {
        fileSystem.copy {
            it.includeEmptyDirs = false
            it.from(from)
            if (relativeResourcePlacement.isPresent) {
                it.into(outputDirectory.dir(relativeResourcePlacement.get().path))
            } else {
                it.into(outputDirectory)
            }
        }
    }
}

/*
  There is a dirty fix for the problem:

  Reason: Task ':generateDemoDebugUnitTestLintModel' uses this output of task ':generateResourceAccessorsForAndroidUnitTest' without declaring an explicit or implicit dependency. This can lead to incorrect results being produced, depending on what order the tasks are executed.

  Possible solutions:
    1. Declare task ':generateResourceAccessorsForAndroidUnitTest' as an input of ':generateDemoDebugUnitTestLintModel'.
    2. Declare an explicit dependency on ':generateResourceAccessorsForAndroidUnitTest' from ':generateDemoDebugUnitTestLintModel' using Task#dependsOn.
    3. Declare an explicit dependency on ':generateResourceAccessorsForAndroidUnitTest' from ':generateDemoDebugUnitTestLintModel' using Task#mustRunAfter.
 */
internal fun Project.fixAndroidLintTaskDependencies() {
    tasks.matching {
        it is AndroidLintAnalysisTask || it is LintModelWriterTask
    }.configureEach {
        it.mustRunAfter(tasks.withType(GenerateResourceAccessorsTask::class.java))
    }
}
