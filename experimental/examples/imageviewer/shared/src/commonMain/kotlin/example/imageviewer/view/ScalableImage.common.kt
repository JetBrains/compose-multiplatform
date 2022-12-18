package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.model.ContentState

@Composable
internal expect fun ScalableImage(modifier: Modifier, image: ImageBitmap)
