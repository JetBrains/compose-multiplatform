package org.jetbrains.compose.internal.publishing

import java.io.File

data class ModuleToUpload(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val localDir: File
) {
    internal fun listFiles(): Array<File> =
        localDir.listFiles() ?: emptyArray()

    internal val coordinate: String
        get() = "$groupId:$artifactId:$version"
}