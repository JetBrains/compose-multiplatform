package org.jetbrains.compose.resources

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.internal.lint.AndroidLintAnalysisTask
import com.android.build.gradle.internal.lint.LintModelWriterTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Copy
import org.jetbrains.compose.internal.utils.dir
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

internal fun Project.getAndroidVariantComposeResources(
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

internal fun Project.getKgpAndroidVariantComposeResources(
    variant: Variant
): FileCollection = project.files({
    val taskName = "${variant.name}AssetsCopyForAGP"
    if (tasks.names.contains(taskName)) tasks.named(taskName).map { it.outputs.files }
    else files()
})

internal fun Project.configureAndroidComposeResources(
    getVariantComposeResources: (Variant) -> FileCollection
) {
    //copy all compose resources to android assets
    val androidComponents = project.extensions.findByType(AndroidComponentsExtension::class.java) ?: return
    androidComponents.onVariants { variant ->
        val variantAssets = getVariantComposeResources(variant)
        val variantAssetsDir = layout.buildDirectory.dir(RES_GEN_DIR).dir(variant.name + "AndroidAssets")

        val copyVariantAssets = tasks.register(
            "copy${variant.name.uppercaseFirstChar()}ComposeResourcesToAndroidAssets",
            Copy::class.java
        ) { task ->
            task.from(variantAssets)
            task.into(variantAssetsDir)
        }

        //https://issuetracker.google.com/348208777
        val staticDir = variantAssetsDir.get().asFile
        staticDir.mkdirs()
        variant.sources.assets?.addStaticSourceDirectory(staticDir.path)
        tasks.configureEach { task ->
            if (task.name == "merge${variant.name.uppercaseFirstChar()}Assets") {
                task.dependsOn(copyVariantAssets)
            }
            //fix task dependencies for AndroidStudio preview
            if (task.name == "compile${variant.name.uppercaseFirstChar()}Sources") {
                task.dependsOn(copyVariantAssets)
            }
            //fix linter task dependencies for `build` task
            if (task is AndroidLintAnalysisTask || task is LintModelWriterTask) {
                task.mustRunAfter(copyVariantAssets)
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