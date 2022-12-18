package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.model.ContentState

@Composable
internal expect fun ScalableImage(image: ImageBitmap, swipeNext: () -> Unit, swipePrevious: () -> Unit)
