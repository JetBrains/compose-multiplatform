package org.jetbrains.compose.desktop.application.dsl

import java.io.Serializable

data class FileAssociation(
    val mimeType: String,
    val extension: String,
    val description: String,
) : Serializable
