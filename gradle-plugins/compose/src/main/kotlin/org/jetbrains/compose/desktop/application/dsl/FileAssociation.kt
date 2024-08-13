package org.jetbrains.compose.desktop.application.dsl

import java.io.File
import java.io.Serializable

internal data class FileAssociation(
    val mimeType: String,
    val extension: String,
    val description: String,
    val iconFile: File?,
) : Serializable
