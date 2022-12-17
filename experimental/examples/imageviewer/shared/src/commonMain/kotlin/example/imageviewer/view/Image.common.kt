package example.imageviewer.view

import androidx.compose.runtime.Composable
import example.imageviewer.model.ContentState

@Composable
internal expect fun Image(content: ContentState)
