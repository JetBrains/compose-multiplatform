/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import de.undercouch.gradle.tasks.download.DownloadAction
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import org.jetbrains.compose.internal.utils.OS
import org.jetbrains.compose.internal.utils.currentOS
import org.jetbrains.compose.internal.utils.findLocalOrGlobalProperty
import java.io.File

internal const val DOWNLOAD_WIX_TOOLSET_TASK_NAME = "downloadWix"
internal const val UNZIP_WIX_TOOLSET_TASK_NAME = "unzipWix"
internal const val WIX_PATH_ENV_VAR = "WIX_PATH"
internal const val DOWNLOAD_WIX_PROPERTY = "compose.desktop.application.downloadWix"
private const val WIX_TOOLSET_URL = "https://github.com/wixtoolset/wix3/releases/download/wix3112rtm/wix311-binaries.zip"

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
    // The archive is shared between all projects (and builds) via Gradle user home,
    // see `DownloadWixToolsetTask` for details.
    val wixDir = project.gradle.gradleUserHomeDir.resolve("compose-jb")
    val fileName = "wix311"
    val zipFile = wixDir.resolve("$fileName.zip")
    val unzipDir = project.layout.buildDirectory.dir(fileName)
    val download = project.tasks.register(DOWNLOAD_WIX_TOOLSET_TASK_NAME, DownloadWixToolsetTask::class.java) { task ->
        task.onlyIf { !zipFile.isFile }
        task.usesService(WixToolsetDownloadLock.registerIfAbsent(project))
        task.configureDownload(WIX_TOOLSET_URL, zipFile)
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

/**
 * Downloads the WiX toolset archive into the cache shared between all projects and builds.
 *
 * The archive is intentionally not declared as a task output (unlike in the `Download` task).
 * Otherwise, in a build with several desktop applications, `downloadWix` tasks of different projects
 * would declare the same output, and an `unzipWix` task would consume an output of another project's task
 * without a dependency on it, which Gradle reports as an error
 * (and such a dependency can't be declared with isolated projects).
 */
@DisableCachingByDefault(because = "Downloads into the cache in Gradle user home")
internal abstract class DownloadWixToolsetTask : DefaultTask() {

    private val downloadAction = DownloadAction(project, this)

    fun configureDownload(url: String, destination: File) {
        downloadAction.src(url)
        downloadAction.dest(destination)
        downloadAction.tempAndMove(true)
        downloadAction.downloadTaskDir(destination.parentFile)
    }

    @TaskAction
    fun download() {
        downloadAction.execute(false).get()
    }
}

/**
 * Makes `downloadWix` tasks of different projects run one by one (`maxParallelUsages = 1`),
 * so the archive is downloaded only once, and the other tasks are skipped.
 */
internal abstract class WixToolsetDownloadLock : BuildService<BuildServiceParameters.None> {
    companion object {
        fun registerIfAbsent(project: Project): Provider<WixToolsetDownloadLock> {
            val serviceClass = WixToolsetDownloadLock::class.java
            val name = "${serviceClass.canonicalName}_${serviceClass.classLoader.hashCode()}"
            return project.gradle.sharedServices.registerIfAbsent(name, serviceClass) { spec ->
                spec.maxParallelUsages.set(1)
            }
        }
    }
}
