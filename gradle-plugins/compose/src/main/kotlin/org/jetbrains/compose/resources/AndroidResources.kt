package org.jetbrains.compose.resources

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.internal.lint.AndroidLintAnalysisTask
import com.android.build.gradle.internal.lint.LintModelWriterTask
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.compose.internal.utils.registerTask
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import javax.inject.Inject

private fun Project.getAndroidVariantComposeResources(
    kotlinExtension: KotlinMultiplatformExtension,
    variant: Variant
): FileCollection = project.files({
    kotlinExtension.targets.withType(KotlinAndroidTarget::class.java).flatMap { androidTarget ->
        androidTarget.compilations.flatMap { compilation ->
            if (compilation.androidVariant.name == variant.name) {
                compilation.allKotlinSourceSets.map { kotlinSourceSet ->
                    getPreparedComposeResourcesDir(kotlinSourceSet)
                }
            } else emptyList()
        }
    }
})

internal fun Project.configureAndroidComposeResources() {
    //copy all compose resources to android assets
    val androidComponents = project.extensions.findByType(AndroidComponentsExtension::class.java) ?: return
    val kotlinExtension = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return
    androidComponents.onVariants { variant ->
        val camelVariantName = variant.name.uppercaseFirstChar()
        val variantAssets = getAndroidVariantComposeResources(kotlinExtension, variant)

        val copyVariantAssets = registerTask<CopyResourcesToAndroidAssetsTask>(
            "copy${camelVariantName}ComposeResourcesToAndroidAssets"
        ) {
            from.set(variantAssets)
        }

        variant.sources.assets?.addGeneratedSourceDirectory(
            copyVariantAssets,
            CopyResourcesToAndroidAssetsTask::outputDirectory
        )
        tasks.configureEach { task ->
            //fix agp task dependencies for AndroidStudio preview
            if (task.name == "compile${camelVariantName}Sources") {
                task.dependsOn(copyVariantAssets)
            }
            //fix linter task dependencies for `build` task
            if (task is AndroidLintAnalysisTask || task is LintModelWriterTask) {
                task.mustRunAfter(copyVariantAssets)
            }
        }
    }
}

//Copy task doesn't work with 'variant.sources?.assets?.addGeneratedSourceDirectory' API
internal abstract class CopyResourcesToAndroidAssetsTask : DefaultTask() {
    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    abstract val from: Property<FileCollection>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun action() {
        fileSystem.copy {
            it.includeEmptyDirs = false
            it.from(from)
            it.into(outputDirectory)
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

internal fun Project.fixKgpAndroidPreviewTaskDependencies() {
    val androidComponents = project.extensions.findByType(AndroidComponentsExtension::class.java) ?: return
    androidComponents.onVariants { variant ->
        tasks.configureEach { task ->
            if (task.name == "compile${variant.name.uppercaseFirstChar()}Sources") {
                task.dependsOn("${variant.name}AssetsCopyForAGP")
            }
        }
    }
}