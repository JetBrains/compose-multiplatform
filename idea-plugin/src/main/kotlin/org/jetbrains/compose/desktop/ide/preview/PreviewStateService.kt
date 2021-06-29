/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.FrameConfig
import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.PreviewManager
import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.PreviewManagerImpl
import java.awt.Dimension
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener

@Service
class PreviewStateService : Disposable {
    private var myPanel: PreviewPanel? = null
    private val previewManager: PreviewManager = PreviewManagerImpl { frame ->
        ByteArrayInputStream(frame.bytes).use { input ->
            val image = ImageIO.read(input)
            myPanel?.previewImage(image, Dimension(frame.width, frame.height))
        }
    }
    val gradleCallbackPort: Int
        get() = previewManager.gradleCallbackPort

    private val myListener = object : AncestorListener {
        private fun updateFrameSize(c: JComponent) {
            val frameConfig = FrameConfig(
                width = c.width,
                height = c.height,
                scale = null
            )
            previewManager.updateFrameConfig(frameConfig)
        }

        override fun ancestorAdded(event: AncestorEvent) {
            updateFrameSize(event.component)
        }

        override fun ancestorRemoved(event: AncestorEvent) {
        }

        override fun ancestorMoved(event: AncestorEvent) {
            updateFrameSize(event.component)
        }
    }

    override fun dispose() {
        myPanel?.removeAncestorListener(myListener)
        previewManager.close()
    }

    internal fun registerPreviewPanel(panel: PreviewPanel) {
        myPanel = panel
        panel.addAncestorListener(myListener)
    }
}
