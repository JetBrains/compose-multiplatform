package org.jetbrains.compose.resources

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.tasks.AndroidVariantTask
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.jetbrains.compose.internal.utils.registerTask
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

    val copyFonts = registerTask<Copy>("copyFontsToAndroidAssets") {
        includeEmptyDirs = false
        from(commonResourcesDir)
        include("**/font*/*")
        into(androidFontsDir)
        onlyIf { onlyIfProvider.get() }
    }
    androidComponents.onVariants { variant ->
        variant.sources?.assets?.addGeneratedSourceDirectory(
            taskProvider = copyFonts,
            wiredWith = {
                objects.directoryProperty().fileProvider(
                    copyFonts.map { t -> t.destinationDir }
                )
            }
        )
    }
    //fixme: it seems like a problem in AGP, so dirty hack now
    //https://github.com/JetBrains/compose-multiplatform/issues/4085
    tasks.matching { it is AndroidVariantTask }.configureEach { it.dependsOn(copyFonts) }
}