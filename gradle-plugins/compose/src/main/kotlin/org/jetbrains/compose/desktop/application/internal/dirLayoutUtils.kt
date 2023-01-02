/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Provider
import org.jetbrains.compose.internal.utils.dir

internal val Project.jvmDirs: JvmDirectoriesProvider
    get() = JvmDirectoriesProvider(project.layout)

internal fun Task.jvmTmpDirForTask(): Provider<Directory> =
    project.jvmDirs.tmpDir(name)

internal class JvmDirectoriesProvider(
    private val layout: ProjectLayout
) {
    val composeDir: Provider<Directory>
        get() = layout.buildDirectory.dir("compose")

    fun tmpDir(name: String): Provider<Directory> =
        composeDir.dir("tmp/$name")
}