package org.jetbrains.compose.resources

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.compose.internal.utils.registerTask
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import java.io.File

internal fun Project.configureAndroidResources(
    commonResourcesDir: Provider<File>,
    androidFontsDir: Provider<File>,
    onlyIfProvider: Provider<Boolean>
) {
    val androidExtension = project.extensions.findByName("android") as? BaseExtension ?: return
    val androidComponents = project.extensions.findByType(AndroidComponentsExtension::class.java) ?: return

    val androidMainSourceSet = androidExtension.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
    androidMainSourceSet.resources.srcDir(commonResourcesDir)
    androidMainSourceSet.assets.srcDir(androidFontsDir)

    androidComponents.onVariants { variant ->
        val copyFonts = registerTask<CopyAndroidAssetsTask>(
            "copy${variant.name.uppercaseFirstChar()}FontsToAndroidAssets"
        ) {
            includeEmptyDirs = false
            from(commonResourcesDir)
            include("**/font*/*")
            onlyIf { onlyIfProvider.get() }
        }
        variant.sources?.assets?.addGeneratedSourceDirectory(
            taskProvider = copyFonts,
            wiredWith = CopyAndroidAssetsTask::outputDirectory
        )
    }
}

//https://github.com/JetBrains/compose-multiplatform/issues/4085
private abstract class CopyAndroidAssetsTask : Copy() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    override fun getDestinationDir(): File =
        outputDirectory.get().asFile

    override fun setDestinationDir(destination: File) {
        outputDirectory.set(destination)
    }
}