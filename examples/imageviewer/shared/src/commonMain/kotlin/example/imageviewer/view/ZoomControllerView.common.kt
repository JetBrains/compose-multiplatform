package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import example.imageviewer.model.ScalableState

@Composable
expect fun ZoomControllerView(modifier: Modifier, scalableState: ScalableState)
