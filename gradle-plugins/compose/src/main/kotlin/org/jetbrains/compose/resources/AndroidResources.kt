package org.jetbrains.compose.resources

import com.android.build.api.AndroidPluginVersion
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.internal.lint.AndroidLintAnalysisTask
import com.android.build.gradle.internal.lint.LintModelWriterTask
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.compose.internal.utils.registerTask
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.utils.ObservableSet
import javax.inject.Inject

private val agp_8_1_0 = AndroidPluginVersion(8, 1, 0)

internal fun Project.configureAndroidComposeResources(
    kotlinExtension: KotlinMultiplatformExtension
) {
    //copy all compose resources to android assets
    val androidComponents = project.extensions.findByType(AndroidComponentsExtension::class.java) ?: return
    androidComponents.onVariants { variant ->
        val variantResources = project.files()

        kotlinExtension.targets.withType(KotlinAndroidTarget::class.java).all { androidTarget ->
            androidTarget.compilations.all { compilation: KotlinJvmAndroidCompilation ->
                if (compilation.androidVariant.name == variant.name) {
                    project.logger.info("Configure resources for variant ${variant.name}")
                    (compilation.allKotlinSourceSets as? ObservableSet<KotlinSourceSet>)?.forAll { kotlinSourceSet ->
                        val preparedComposeResources = getPreparedComposeResourcesDir(kotlinSourceSet)
                        variantResources.from(preparedComposeResources)
                    }
                }
            }
        }

        val copyResources = registerTask<CopyResourcesToAndroidAssetsTask>(
            "copy${variant.name.uppercaseFirstChar()}ResourcesToAndroidAssets"
        ) {
            from.set(variantResources)
        }
        variant.sources.assets?.apply {
            addGeneratedSourceDirectory(
                taskProvider = copyResources,
                wiredWith = CopyResourcesToAndroidAssetsTask::outputDirectory
            )

            // https://issuetracker.google.com/348208777
            if (androidComponents.pluginVersion >= agp_8_1_0) {
                // addGeneratedSourceDirectory doesn't mark the output directory as assets hence AS Compose Preview doesn't work
                addStaticSourceDirectory(copyResources.flatMap { it.outputDirectory.asFile }.get().path)
            }

            // addGeneratedSourceDirectory doesn't run the copyResources task during AS Compose Preview build
            tasks.configureEach { task ->
                if (task.name == "compile${variant.name.uppercaseFirstChar()}Sources") {
                    task.dependsOn(copyResources)
                }
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
        it.mustRunAfter(tasks.withType(CopyResourcesToAndroidAssetsTask::class.java))
    }
}

// https://issuetracker.google.com/348208777
internal fun Project.configureAndroidAssetsForPreview() {
    val androidComponents = project.extensions.findByType(AndroidComponentsExtension::class.java) ?: return
    androidComponents.onVariants { variant ->
        variant.sources.assets?.apply {
            val kgpCopyAssetsTaskName = "${variant.name}AssetsCopyForAGP"

            if (androidComponents.pluginVersion >= agp_8_1_0) {
                // addGeneratedSourceDirectory doesn't mark the output directory as assets hence AS Compose Preview doesn't work
                tasks.all { task ->
                    if (task.name == kgpCopyAssetsTaskName) {
                        task.outputs.files.forEach { file ->
                            addStaticSourceDirectory(file.path)
                        }
                    }
                }
            }

            // addGeneratedSourceDirectory doesn't run the copyResources task during AS Compose Preview build
            tasks.configureEach { task ->
                if (task.name == "compile${variant.name.uppercaseFirstChar()}Sources") {
                    task.dependsOn(kgpCopyAssetsTaskName)
                }
            }
        }
    }
}