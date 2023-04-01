package example.imageviewer

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(openShareController:(SharedPhoto) -> Unit): UIViewController =
    ComposeUIViewController {
        ImageViewerIos(
            openShareController = openShareController
        )
    }

