/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview.remote

import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener

internal class PreviewStateUpdaterListener(private val state: PreviewState): AncestorListener, WindowFocusListener {
    override fun ancestorAdded(e: AncestorEvent) {
        state.updatePreviewPanelState(e.component)
    }

    override fun ancestorRemoved(e: AncestorEvent) {
        state.updatePreviewPanelState(e.component)
    }

    override fun ancestorMoved(e: AncestorEvent) {
        state.updatePreviewPanelState(e.component)
    }

    override fun windowGainedFocus(e: WindowEvent) {
        state.updateIdeWindowState(e.window)
    }

    override fun windowLostFocus(e: WindowEvent) {
        state.updateIdeWindowState(e.window)
    }
}