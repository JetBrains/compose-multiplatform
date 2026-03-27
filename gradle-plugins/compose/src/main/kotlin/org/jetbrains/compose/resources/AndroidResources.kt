package org.jetbrains.compose.resources

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasAndroidTest
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import com.android.build.api.variant.Sources
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
import org.jetbrains.compose.internal.Version
import org.jetbrains.compose.internal.utils.registerTask
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import java.io.File
import javax.inject.Inject

//copy all compose resources to android assets
internal fun Project.configureAndroidComposeResources(
    agpPluginId: String,
    moduleResourceDir: Provider<File>? = null
) {
    val kotlinExtension = extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return

    if (agpPluginId != AGP_KMP_LIB_ID) {
        extensions.findByType(AndroidComponentsExtension::class.java)?.let { androidComponents ->
            configureAndroidComposeResources(kotlinExtension, androidComponents, moduleResourceDir)
        }
    } else {
        @Suppress("UnstableApiUsage")
        extensions.findByType(KotlinMultiplatformAndroidComponentsExtension::class.java)?.let { androidComponents ->
            configureAndroidComposeResources(kotlinExtension, androidComponents, moduleResourceDir)
        }
    }
}

private fun Project.configureAndroidComposeResources(
    kotlinExtension: KotlinMultiplatformExtension,
    androidComponents: AndroidComponentsExtension<*, *, *>,
    moduleResourceDir: Provider<File>?
) {
    logger.info("Configure compose resources with AndroidComponentsExtension")
    androidComponents.onVariants { variant ->
        val componentAssets = getAndroidComponentComposeResources(kotlinExtension, variant.name)
        configureGeneratedAndroidComponentAssets(
            variant.name,
            variant.sources,
            componentAssets,
            moduleResourceDir
        )

        if (variant is HasAndroidTest) {
            variant.androidTest?.let { androidTest ->
                val androidTestAssets = getAndroidComponentComposeResources(kotlinExtension, androidTest.name)
                configureGeneratedAndroidComponentAssets(
                    androidTest.name,
                    androidTest.sources,
                    androidTestAssets,
                    moduleResourceDir
                )
            }
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

private const val AGP_8_10 = "8.10"

@Suppress("UnstableApiUsage")
private fun Project.configureAndroidComposeResources(
    kotlinExtension: KotlinMultiplatformExtension,
    androidComponents: KotlinMultiplatformAndroidComponentsExtension,
    moduleResourceDir: Provider<File>?
) {

    // AGP 8.10 introduced new onVariantS {} API
    // AGP 9.0.0-alpha01 removed onVariant {} API
    // https://github.com/JetBrains/compose-multiplatform/pull/5385

    val agpVersion = Version.fromString(androidComponents.pluginVersion.version)
    if (agpVersion >= Version.fromString(AGP_8_10)) {
        logger.info("Configure compose resources with KotlinMultiplatformAndroidComponentsExtension")
        androidComponents.onVariants { variant ->
            val variantAssets = getAndroidKmpComponentComposeResources(kotlinExtension, variant.name)
            configureGeneratedAndroidComponentAssets(
                variant.name,
                variant.sources,
                variantAssets,
                moduleResourceDir
            )

            variant.androidTest?.let { androidTest ->
                val androidTestAssets = getAndroidKmpComponentComposeResources(kotlinExtension, androidTest.name)
                configureGeneratedAndroidComponentAssets(
                    androidTest.name,
                    androidTest.sources,
                    androidTestAssets,
                    moduleResourceDir
                )
            }
        }
    } else {
        logger.info("Configure compose resources with outdated KotlinMultiplatformAndroidComponentsExtension < 8.10")
        androidComponents.onVariant { variant ->
            val variantAssets = getAndroidKmpComponentComposeResources(kotlinExtension, variant.name)
            configureGeneratedAndroidComponentAssets(
                variant.name,
                variant.sources,
                variantAssets,
                moduleResourceDir
            )

            variant.androidTest?.let { androidTest ->
                val androidTestAssets = getAndroidKmpComponentComposeResources(kotlinExtension, androidTest.name)
                configureGeneratedAndroidComponentAssets(
                    androidTest.name,
                    androidTest.sources,
                    androidTestAssets,
                    moduleResourceDir
                )
            }
        }
    }
}

@Suppress("UnstableApiUsage")
private fun Project.getAndroidKmpComponentComposeResources(
    kotlinExtension: KotlinMultiplatformExtension,
    componentName: String
): FileCollection = project.files({
    kotlinExtension.targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
        .flatMap { androidTarget ->
            androidTarget.compilations.flatMap { compilation ->
                if (compilation.componentName == componentName) {
                    compilation.allKotlinSourceSets.map { kotlinSourceSet ->
                        getPreparedComposeResourcesDir(kotlinSourceSet)
                    }
                } else emptyList()
            }
        }
})

private fun Project.configureGeneratedAndroidComponentAssets(
    componentName: String,
    componentSources: Sources,
    componentAssets: FileCollection,
    moduleResourceDir: Provider<File>?
) {
    logger.info("Configure $componentName resources for 'android' target")

    val camelComponentName = componentName.uppercaseFirstChar()
    val copyComponentAssets = registerTask<CopyResourcesToAndroidAssetsTask>(
        "copy${camelComponentName}ComposeResourcesToAndroidAssets"
    ) {
        from.set(componentAssets)
        moduleResourceDir?.let { relativeResourcePlacement.set(it) }
    }

    componentSources.assets?.addGeneratedSourceDirectory(
        copyComponentAssets,
        CopyResourcesToAndroidAssetsTask::outputDirectory
    )
    tasks.configureEach { task ->
        //fix agp task dependencies for AndroidStudio preview
        if (task.name == "package${camelComponentName}Resources") {
            task.dependsOn(copyComponentAssets)
        }
        //fix linter task dependencies for `build` task
        if (task is AndroidLintAnalysisTask || task is LintModelWriterTask) {
            task.mustRunAfter(copyComponentAssets)
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
