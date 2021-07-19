/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLoadingPanel
import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.*
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener

@Service
class PreviewStateService : Disposable {
    private val previewListener = CompositePreviewListener()
    private val previewManager: PreviewManager = PreviewManagerImpl(previewListener)
    val gradleCallbackPort: Int
        get() = previewManager.gradleCallbackPort

    override fun dispose() {
        previewManager.close()
    }

    internal fun registerPreviewPanels(
        previewPanel: PreviewPanel,
        loadingPanel: JBLoadingPanel
    ) {
        val previewResizeListener = PreviewResizeListener(previewManager)
        previewPanel.addAncestorListener(previewResizeListener)
        Disposer.register(this) { previewPanel.removeAncestorListener(previewResizeListener) }

        previewListener.addListener(PreviewPanelUpdater(previewPanel))
        previewListener.addListener(LoadingPanelUpdater(loadingPanel))
    }

    internal fun buildStarted() {
        previewListener.onNewBuildRequest()
    }

    internal fun buildFinished(success: Boolean) {
        previewListener.onFinishedBuild(success)
    }
}

private class PreviewResizeListener(private val previewManager: PreviewManager) : AncestorListener {
    private fun updateFrameSize(c: JComponent) {
        val frameConfig = FrameConfig(
            width = c.width,
            height = c.height,
            scale = c.graphicsConfiguration.defaultTransform.scaleX
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

private class PreviewPanelUpdater(private val panel: PreviewPanel) : PreviewListenerBase() {
    override fun onRenderedFrame(frame: RenderedFrame) {
        panel.previewImage(frame.image, frame.dimension)
    }
}

private class LoadingPanelUpdater(private val panel: JBLoadingPanel) : PreviewListenerBase() {
    override fun onNewBuildRequest() {
        panel.setLoadingText("Building project")
        panel.startLoading()
    }

    override fun onFinishedBuild(success: Boolean) {
        panel.stopLoading()
    }

    override fun onNewRenderRequest(previewRequest: FrameRequest) {
        panel.setLoadingText("Rendering preview")
        panel.startLoading()
    }

    override fun onRenderedFrame(frame: RenderedFrame) {
        panel.stopLoading()
    }
}