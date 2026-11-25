/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import org.jetbrains.compose.internal.utils.OS
import org.jetbrains.compose.internal.utils.currentOS
import org.jetbrains.compose.internal.utils.findLocalOrGlobalProperty
import java.io.File

internal const val DOWNLOAD_WIX_TOOLSET_TASK_NAME = "downloadWix"
internal const val UNZIP_WIX_TOOLSET_TASK_NAME = "unzipWix"
internal const val WIX_PATH_ENV_VAR = "WIX_PATH"
internal const val DOWNLOAD_WIX_PROPERTY = "compose.desktop.application.downloadWix"

internal fun JvmApplicationContext.configureWix() {
    check(currentOS == OS.Windows) { "Should not be called for non-Windows OS: $currentOS" }

    val wixPath = System.getenv()[WIX_PATH_ENV_VAR]
    if (wixPath != null) {
        val wixDir = File(wixPath)
        check(wixDir.isDirectory) { "$WIX_PATH_ENV_VAR value is not a valid directory: $wixDir" }
        project.eachWindowsPackageTask {
            wixToolsetDir.set(wixDir)
        }
        return
    }

    val disableWixDownload = project.findLocalOrGlobalProperty(DOWNLOAD_WIX_PROPERTY).map { it == "false" }
    if (disableWixDownload.get()) return

    // The tasks are registered in the project itself (not in the root project)
    // to be compatible with Gradle isolated projects.
    // The archive is shared between all projects via Gradle user home.
    // Gradle doesn't run tasks with overlapping outputs in parallel,
    // so `downloadWix` tasks of different projects don't race for the archive.
    val wixDir = project.gradle.gradleUserHomeDir.resolve("compose-jb")
    val fileName = "wix311"
    val zipFile = wixDir.resolve("$fileName.zip")
    val unzipDir = project.layout.buildDirectory.dir(fileName)
    val download = project.tasks.register(DOWNLOAD_WIX_TOOLSET_TASK_NAME, Download::class.java) { task ->
        task.onlyIf { !zipFile.isFile }
        task.src("https://github.com/wixtoolset/wix3/releases/download/wix3112rtm/wix311-binaries.zip")
        task.dest(zipFile)
    }
    val unzip = project.tasks.register(UNZIP_WIX_TOOLSET_TASK_NAME, Copy::class.java) { task ->
        task.dependsOn(download)
        task.from(project.zipTree(zipFile))
        task.into(unzipDir)
    }
    project.eachWindowsPackageTask {
        dependsOn(unzip)
        wixToolsetDir.set(unzipDir)
    }
}

private fun Project.eachWindowsPackageTask(fn: AbstractJPackageTask.() -> Unit) {
    tasks.withType(AbstractJPackageTask::class.java).configureEach { packageTask ->
        if (packageTask.targetFormat.isCompatibleWith(OS.Windows)) {
            packageTask.fn()
        }
    }
}
