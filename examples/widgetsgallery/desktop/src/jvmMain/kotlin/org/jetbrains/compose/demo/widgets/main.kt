package org.jetbrains.compose.demo.widgets

import androidx.compose.desktop.Window
import androidx.compose.ui.unit.IntSize
import org.jetbrains.compose.demo.widgets.ui.MainView
import java.awt.Dimension
import java.awt.Toolkit
import javax.swing.SwingUtilities.invokeLater

fun main() {
    invokeLater {
        Window(
            title = "Widgets Gallery",
            size = getPreferredWindowSize(600, 800),
        ) {
            MainView()
        }
    }
}

private fun getPreferredWindowSize(desiredWidth: Int, desiredHeight: Int): IntSize {
    val screenSize: Dimension = Toolkit.getDefaultToolkit().screenSize
    val preferredWidth: Int = (screenSize.width * 0.8f).toInt()
    val preferredHeight: Int = (screenSize.height * 0.8f).toInt()
    val width: Int = if (desiredWidth < preferredWidth) desiredWidth else preferredWidth
    val height: Int = if (desiredHeight < preferredHeight) desiredHeight else preferredHeight
    return IntSize(width, height)
}
