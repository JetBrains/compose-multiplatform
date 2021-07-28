package example.imageviewer

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import example.imageviewer.model.ContentState
import example.imageviewer.style.icAppRounded
import example.imageviewer.utils.getPreferredWindowSize
import example.imageviewer.view.BuildAppUI
import example.imageviewer.view.SplashUI

fun main() = application {
    val state = rememberWindowState()
    val content = remember {
        ContentState.applyContent(
            state,
            "https://raw.githubusercontent.com/JetBrains/compose-jb/master/artwork/imageviewerrepo/fetching.list"
        )
    }

    val icon = icAppRounded()

    if (content.isAppReady()) {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Image Viewer",
            state = WindowState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = getPreferredWindowSize(800, 1000)
            ),
            icon = icon
        ) {
            MaterialTheme {
                BuildAppUI(content)
            }
        }
    } else {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Image Viewer",
            state = WindowState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = getPreferredWindowSize(800, 300)
            ),
            undecorated = true,
            icon = icon,
        ) {
            MaterialTheme {
                SplashUI()
            }
        }
    }
}