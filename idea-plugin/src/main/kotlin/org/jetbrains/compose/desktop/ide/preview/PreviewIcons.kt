/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.openapi.util.IconLoader

object PreviewIcons {
    private fun load(path: String) = IconLoader.getIcon(path, PreviewIcons::class.java)

    val COMPOSE = load("/icons/compose/compose.svg")
    val RUN_PREVIEW = load("/icons/compose/runPreview.svg")
}