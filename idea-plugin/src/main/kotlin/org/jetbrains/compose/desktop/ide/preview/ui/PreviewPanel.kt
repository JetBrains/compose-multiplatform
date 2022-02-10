/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.StatusText
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities

internal class PreviewPanel(private val myProject: Project) : JBPanel<PreviewPanel>() {
    sealed class PreviewPanelState {
        data class Image(val image: BufferedImage, val dimension: Dimension) : PreviewPanelState()
        class Error(val error: String) : PreviewPanelState()
    }
    private val myState = AtomicReference<PreviewPanelState>()
    private val myStatusText = object : StatusText(this) {
        override fun isStatusVisible(): Boolean {
            return myState.get() is PreviewPanelState.Error
        }
    }

    init {
        SwingUtilities.invokeLater {
            myStatusText.initStatusText()
        }
    }

    fun StatusText.initStatusText() {
        clear()
        appendLine(
            AllIcons.General.Error,
            "Preview rendering encountered an error",
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            null
        )
        appendLine(
            "Show details",
            SimpleTextAttributes.LINK_ATTRIBUTES
        ) {
            val errorText = (myState.get() as? PreviewPanelState.Error)?.error
            showTextDialog("Preview Error Details", errorText.orEmpty(), myProject)
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        when (val state = myState.get()) {
            is PreviewPanelState.Image -> {
                val (image, dimension) = state
                val w = dimension.width
                val h = dimension.height
                g.color = Color.WHITE
                g.fillRect(0, 0, w, h)
                g.drawImage(image, 0, 0, w, h, null)
            }
            is PreviewPanelState.Error -> {
                myStatusText.paint(this, g)
            }
        }
    }

    fun previewImage(image: BufferedImage, imageDimension: Dimension) {
        myState.set(PreviewPanelState.Image(image, imageDimension))
        SwingUtilities.invokeLater {
            repaint()
        }
    }

    fun error(error: String) {
        myState.set(PreviewPanelState.Error(error))
        SwingUtilities.invokeLater {
            repaint()
        }
    }

    override fun getPreferredSize(): Dimension? =
        (myState.get() as? PreviewPanelState.Image)?.dimension ?: super.getPreferredSize()
}