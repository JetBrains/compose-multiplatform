package example.imageviewer

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import example.imageviewer.model.ContentState
import example.imageviewer.style.icAppRounded
import example.imageviewer.utils.getPreferredWindowSize
import example.imageviewer.view.ImageViewerDesktop
import example.imageviewer.view.SplashUI

fun main() = application {
    MaterialTheme {
        ImageViewerDesktop()
    }
}
