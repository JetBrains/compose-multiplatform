/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal.files

import java.io.File

object SimpleFileCopyingProcessor : FileCopyingProcessor {
    override fun copy(source: File, target: File) {
        source.copyTo(target, overwrite = true)
    }
}