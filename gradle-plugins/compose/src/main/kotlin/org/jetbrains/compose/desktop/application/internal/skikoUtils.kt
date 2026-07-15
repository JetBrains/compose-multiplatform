/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.jetbrains.compose.desktop.application.internal.files.isJarFile
import org.jetbrains.compose.internal.utils.OS
import org.jetbrains.compose.internal.utils.currentArch
import org.jetbrains.compose.internal.utils.currentOS
import java.io.File

internal fun File.isSkikoAwtRuntimeJar(): Boolean =
    isJarFile && name.startsWith("skiko-") && "-awt-runtime" in name

internal fun isSkikoNativeEntry(entryName: String): Boolean =
    entryName.skikoFileName().isSkikoNative()

internal fun shouldKeepSkikoEntry(entryName: String): Boolean {
    val fileName = entryName.skikoFileName()
    return when {
        !fileName.isSkikoNative() -> true
        fileName == "icudtl.dat" -> currentOS == OS.Windows
        else -> fileName.contains("-${currentOS.id}-${currentArch.id}")
    }
}

private fun String.skikoFileName(): String =
    removeSuffix(".sha256").substringAfterLast("/")

private fun String.isSkikoNative(): Boolean = when {
    this == "icudtl.dat" -> true
    startsWith("libskiko") -> endsWith(".so") || endsWith(".dylib")
    startsWith("skiko") -> endsWith(".dll")
    else -> false
}
