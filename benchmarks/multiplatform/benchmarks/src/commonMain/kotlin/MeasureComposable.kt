import androidx.compose.runtime.Composable

/**
 * An opaque graphics context for benchmark rendering.
 * Platform-specific implementations provide GPU surface creation and synchronization.
 */
expect interface GraphicsContext

/**
 * Measures the performance of a composable in simple and vsync_emulation modes.
 * Not supported on Android (simple and vsync_emulation modes are not available).
 */
internal expect suspend fun measureComposable(
    name: String,
    warmupCount: Int,
    frameCount: Int,
    width: Int,
    height: Int,
    targetFps: Int,
    graphicsContext: GraphicsContext?,
    content: @Composable () -> Unit
): BenchmarkResult
