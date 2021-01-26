package example.imageviewer

import androidx.compose.desktop.DesktopTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.remember
import example.imageviewer.model.ContentState
import example.imageviewer.style.icAppRounded
import example.imageviewer.utils.Application
import example.imageviewer.utils.getPreferredWindowSize
import example.imageviewer.view.BuildAppUI
import example.imageviewer.view.SplashUI

fun main() {
    val content = ContentState.applyContent(
        "https://raw.githubusercontent.com/JetBrains/compose-jb/master/artwork/imageviewerrepo/fetching.list"
    )

    Application {
        val icon = remember(::icAppRounded)

        if (content.isContentReady()) {
            ComposableWindow(
                title = "Image Viewer",
                size = getPreferredWindowSize(800, 1000),
                icon = icon
            ) {
                MaterialTheme {
                    DesktopTheme {
                        BuildAppUI(content)
                    }
                }
            }
        } else {
            ComposableWindow(
                title = "Image Viewer",
                size = getPreferredWindowSize(800, 300),
                undecorated = true,
                icon = icon,
            ) {
                MaterialTheme {
                    DesktopTheme {
                        SplashUI()
                    }
                }
            }
        }
    }
}
