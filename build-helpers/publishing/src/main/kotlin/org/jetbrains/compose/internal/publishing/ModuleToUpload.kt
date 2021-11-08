/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

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