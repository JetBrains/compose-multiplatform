package example.imageviewer.view

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import example.imageviewer.model.ScalableState

@Composable
internal actual fun BoxScope.ZoomControllerView(scalableState: ScalableState) {
    // No need for additional ZoomControllerView for Android
}
