package example.imageviewer.view

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import example.imageviewer.Dependencies

internal val LocalDependencies: ProvidableCompositionLocal<Dependencies> =
    staticCompositionLocalOf { error("Dependencies not initialized") }