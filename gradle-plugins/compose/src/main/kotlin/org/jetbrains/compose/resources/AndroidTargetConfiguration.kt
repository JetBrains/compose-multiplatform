package org.jetbrains.compose.resources

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.tasks.MergeSourceSetFolders
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
    val androidMainSourceSet = androidExtension.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
    androidMainSourceSet.resources.srcDir(commonResourcesDir)
    androidMainSourceSet.assets.srcDir(androidFontsDir)

    val copyFonts = registerTask<Copy>("copyFontsToAndroidAssets") {
        includeEmptyDirs = false
        from(commonResourcesDir)
        include("**/fonts/*")
        into(androidFontsDir)
        onlyIf { onlyIfProvider.get() }
    }

    tasks.withType(MergeSourceSetFolders::class.java).all {
        it.dependsOn(copyFonts)
    }
}