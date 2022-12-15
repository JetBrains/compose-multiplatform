package example.imageviewer

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.window.application
import example.imageviewer.view.ImageViewerDesktop

fun main() = application {
    MaterialTheme {
        ImageViewerDesktop()
    }
}
