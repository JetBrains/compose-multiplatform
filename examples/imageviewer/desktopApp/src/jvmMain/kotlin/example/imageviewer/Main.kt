package example.imageviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.application
import example.imageviewer.view.ImageViewerDesktop
import java.awt.Color.CYAN
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JLayeredPane
import javax.swing.JLayeredPane.DEFAULT_LAYER
import javax.swing.JLayeredPane.POPUP_LAYER
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.OverlayLayout
import javax.swing.SwingUtilities

fun main() = application {
    ImageViewerDesktop()
}

fun main2() {
    System.setProperty("compose.swing.render.on.graphics", "true")
    SwingUtilities.invokeLater {
        val composePanel = ComposePanel().apply {
            setContent {
                Box(modifier = Modifier.background(Color.Black).fillMaxSize())
            }
        }


        val popup = object : JComponent() {
            init {
                isOpaque = false
            }

            override fun paintComponent(g: Graphics?) {
                val scratchGraphics = g?.create() as? Graphics2D ?: return
                try {
                    scratchGraphics.color = java.awt.Color.WHITE
                    scratchGraphics.fillRoundRect(5, 5, 90, 50, 16, 16)

                    scratchGraphics.color = java.awt.Color.BLACK
                    scratchGraphics.drawString("Popup", 30, 30)
                } finally {
                    scratchGraphics.dispose()
                }
            }
        }


        val rightPanel = JLayeredPane()
        rightPanel.layout = OverlayLayout(rightPanel)
        rightPanel.add(composePanel)
        rightPanel.add(popup)

        rightPanel.setLayer(composePanel, DEFAULT_LAYER)
        rightPanel.setLayer(popup, POPUP_LAYER)


        val leftPanel = JPanel().apply { background = CYAN }


        val splitter = JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            true,
            leftPanel,
            rightPanel
        ).apply {
            setDividerLocation(500)
        }


        JFrame().apply {
            add(splitter)
            setSize(600, 600)
            isVisible = true
        }
    }
}
