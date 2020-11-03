package org.jetbrains.compose.desktop.application.internal

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import java.io.File

internal const val DOWNLOAD_WIX_TOOLSET_TASK_NAME = "downloadWix"
internal const val UNZIP_WIX_TOOLSET_TASK_NAME = "unzipWix"
internal const val WIX_PATH_ENV_VAR = "WIX_PATH"
internal const val DOWNLOAD_WIX_PROPERTY = "compose.desktop.application.downloadWix"

internal fun Project.configureWix() {
    if (currentOS != OS.Windows) return

    val wixPath = System.getenv()[WIX_PATH_ENV_VAR]
    if (wixPath != null) {
        val wixDir = File(wixPath)
        check(wixDir.isDirectory) { "$WIX_PATH_ENV_VAR value is not a valid directory: $wixDir" }
        eachWindowsPackageTask {
            wixToolsetDir.set(wixDir)
        }
        return
    }

    if (project.findProperty(DOWNLOAD_WIX_PROPERTY) == "false") return

    val root = project.rootProject
    val wixDir = root.buildDir.resolve("wixToolset")
    val zipFile = wixDir.resolve("wix311.zip")
    val unzipDir = wixDir.resolve("unpacked")
    val download = root.tasks.maybeCreate(DOWNLOAD_WIX_TOOLSET_TASK_NAME, Download::class.java).apply {
        onlyIf { !zipFile.isFile }
        src("https://github.com/wixtoolset/wix3/releases/download/wix3112rtm/wix311-binaries.zip")
        dest(zipFile)
    }
    val unzip = root.tasks.maybeCreate(UNZIP_WIX_TOOLSET_TASK_NAME, Copy::class.java).apply {
        dependsOn(download)
        from(zipTree(zipFile))
        destinationDir = unzipDir
    }
    eachWindowsPackageTask {
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