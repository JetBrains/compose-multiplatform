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
import org.jetbrains.compose.internal.utils.ioFile
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

    val root = project.rootProject
    val wixDir = project.gradle.gradleUserHomeDir.resolve("compose-jb")
    val fileName = "wix311"
    val zipFile = wixDir.resolve("$fileName.zip")
    val unzipDir = root.layout.buildDirectory.dir(fileName)
    val download = root.tasks.findByName(DOWNLOAD_WIX_TOOLSET_TASK_NAME) ?: root.tasks.maybeCreate(
        DOWNLOAD_WIX_TOOLSET_TASK_NAME,
        Download::class.java
    ).apply {
        onlyIf { !zipFile.isFile }
        src("https://github.com/wixtoolset/wix3/releases/download/wix3112rtm/wix311-binaries.zip")
        dest(zipFile)
    }
    val unzip = root.tasks.findByName(UNZIP_WIX_TOOLSET_TASK_NAME) ?: root.tasks.maybeCreate(
        UNZIP_WIX_TOOLSET_TASK_NAME,
        Copy::class.java
    ).apply {
        dependsOn(download)
        from(project.zipTree(zipFile))
        destinationDir = unzipDir.ioFile
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
