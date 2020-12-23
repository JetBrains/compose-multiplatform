package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import java.io.File

internal val <T : FileSystemLocation> Provider<T>.ioFile: File
    get() = get().asFile

internal val <T : FileSystemLocation> Provider<T>.ioFileOrNull: File?
    get() = orNull?.asFile
