package org.jetbrains.compose.desktop.application.internal.files

import java.io.File

object SimpleFileCopyingProcessor : FileCopyingProcessor {
    override fun copy(source: File, target: File) {
        source.copyTo(target, overwrite = true)
    }
}