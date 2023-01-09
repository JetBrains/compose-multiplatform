/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.compose.internal.utils.OS
import org.jetbrains.compose.internal.utils.currentOS
import org.jetbrains.compose.internal.utils.executableName
import org.jetbrains.compose.internal.utils.ioFile
import org.jetbrains.compose.desktop.tasks.AbstractComposeDesktopTask
import javax.inject.Inject

// Custom task is used instead of Exec, because Exec does not support
// lazy configuration yet. Lazy configuration is needed to
// calculate appImageDir after the evaluation of createApplicationImage
abstract class AbstractRunDistributableTask @Inject constructor(
    createApplicationImage: TaskProvider<AbstractJPackageTask>
) : AbstractComposeDesktopTask() {
    @get:InputDirectory
    internal val appImageRootDir: Provider<Directory> = createApplicationImage.flatMap { it.destinationDir }

    @get:Input
    internal val packageName: Provider<String> = createApplicationImage.flatMap { it.packageName }

    @TaskAction
    fun run() {
        val appDir = appImageRootDir.ioFile.let { appImageRoot ->
            val files = appImageRoot.listFiles()
                // Sometimes ".DS_Store" files are created on macOS, so ignore them.
                ?.filterNot { it.name == ".DS_Store" }
            if (files == null || files.isEmpty()) {
                error("Could not find application image: $appImageRoot is empty!")
            } else if (files.size > 1) {
                error("Could not find application image: $appImageRoot contains multiple children [${files.joinToString(", ")}]")
            } else files.single()
        }
        val appExecutableName = executableName(packageName.get())
        val (workingDir, executable) = when (currentOS) {
            OS.Linux ->  appDir to "bin/$appExecutableName"
            OS.Windows -> appDir to appExecutableName
            OS.MacOS -> appDir.resolve("Contents") to "MacOS/$appExecutableName"
        }

        execOperations.exec { spec ->
            spec.workingDir(workingDir)
            spec.executable(workingDir.resolve(executable).absolutePath)
        }.assertNormalExitValue()
    }
}