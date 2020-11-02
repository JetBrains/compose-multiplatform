package example.imageviewer

import androidx.compose.desktop.DesktopTheme
import androidx.compose.desktop.Window
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.remember
import example.imageviewer.utils.getPreferredWindowSize
import example.imageviewer.view.BuildAppUI
import example.imageviewer.model.ContentState
import example.imageviewer.model.ImageRepository
import example.imageviewer.style.icAppRounded

fun main() {

    Window(
        title = "ImageViewer",
        size = getPreferredWindowSize(800, 1000),
        icon = icAppRounded()
    ) {
        val content = ContentState.applyContent(
            "https://spvessel.com/iv/images/fetching.list"
        )
        MaterialTheme {
            DesktopTheme {
                BuildAppUI(content)
            }
        }
    }
}
