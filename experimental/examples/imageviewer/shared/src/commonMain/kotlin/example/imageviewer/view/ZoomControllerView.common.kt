package example.imageviewer.view

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import example.imageviewer.model.ScalableState

@Composable
internal expect fun BoxScope.ZoomControllerView(scalableState: ScalableState)
