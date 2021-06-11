/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview.remote

import java.awt.Window
import javax.swing.JComponent

internal class PreviewState(
    isPanelShown: Boolean = false,
    viewPort: PreviewViewPort = PreviewViewPort(),
    isIdeInFocus: Boolean = false
) {
    var isPanelShown: Boolean = isPanelShown
        @Synchronized get
        private set
    var viewPort: PreviewViewPort = viewPort
        @Synchronized get
        private set
    var isIdeInFocus: Boolean = isIdeInFocus
        @Synchronized get
        private set

    constructor(other: PreviewState) : this(other.isPanelShown, other.viewPort, other.isIdeInFocus)

    @Synchronized
    fun updatePreviewPanelState(c: JComponent) {
        viewPort = PreviewViewPort(
            x = c.locationOnScreen.x,
            y = c.locationOnScreen.y,
            width = c.width,
            height = c.height
        )
        isPanelShown = c.isVisible
    }

    @Synchronized
    fun updateIdeWindowState(w: Window) {
        isIdeInFocus = w.isFocused
    }
}

