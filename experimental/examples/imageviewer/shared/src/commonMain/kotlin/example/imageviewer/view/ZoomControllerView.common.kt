package example.imageviewer.view

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import example.imageviewer.model.ScalableState

@Composable
internal expect fun ZoomControllerView(modifier: Modifier, scalableState: ScalableState)
