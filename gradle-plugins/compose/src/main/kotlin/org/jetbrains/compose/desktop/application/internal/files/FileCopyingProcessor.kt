package org.jetbrains.compose.desktop.application.internal.files

import java.io.File

internal interface FileCopyingProcessor {
    fun copy(source: File, target: File)
}
