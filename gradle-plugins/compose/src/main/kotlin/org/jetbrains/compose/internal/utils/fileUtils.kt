/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.utils

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.io.File
import java.util.*

internal fun Provider<String>.toDir(project: Project): Provider<Directory> =
    project.layout.dir(map { File(it) })

internal fun Provider<File>.fileToDir(project: Project): Provider<Directory> =
    project.layout.dir(this)

internal fun Provider<Directory>.file(relativePath: String): Provider<RegularFile> =
    map { it.file(relativePath) }

internal fun Provider<Directory>.dir(relativePath: String): Provider<Directory> =
    map { it.dir(relativePath) }

internal val <T : FileSystemLocation> Provider<T>.ioFile: File
    get() = get().asFile

internal val <T : FileSystemLocation> Provider<T>.ioFileOrNull: File?
    get() = orNull?.asFile

internal fun FileSystemOperations.delete(vararg files: Any) {
    delete { it.delete(*files) }
}

internal fun FileSystemOperations.mkdirs(vararg dirs: File) {
    for (dir in dirs) {
        dir.mkdirs()
    }
}

internal fun FileSystemOperations.mkdirs(vararg dirs: Provider<out FileSystemLocation>) {
    mkdirs(*dirs.ioFiles())
}

internal fun FileSystemOperations.clearDirs(vararg dirs: File) {
    delete(*dirs)
    mkdirs(*dirs)
}

internal fun FileSystemOperations.clearDirs(vararg dirs: Provider<out FileSystemLocation>) {
    clearDirs(*dirs.ioFiles())
}

private fun Array<out Provider<out FileSystemLocation>>.ioFiles(): Array<File> =
    let { providers -> Array(size) { i -> providers[i].ioFile } }

internal fun lazyLoadProperties(propertiesFile: File): Lazy<Properties> = lazy {
    loadProperties(propertiesFile)
}

internal fun loadProperties(propertiesFile: File): Properties =
    Properties().apply {
        if (propertiesFile.isFile) {
            propertiesFile.inputStream().use {
                load(it)
            }
        }
    }