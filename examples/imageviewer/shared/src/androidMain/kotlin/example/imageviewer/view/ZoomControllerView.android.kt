package example.imageviewer.view

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import example.imageviewer.model.ScalableState
import androidx.compose.ui.Modifier

@Composable
actual fun ZoomControllerView(modifier: Modifier, scalableState: ScalableState) {
    // No need for additional ZoomControllerView for Android
}
