import androidx.compose.runtime.Composable

/**
 * No-op GraphicsContext for Android.
 * Simple and vsync_emulation modes are not supported on Android.
 */
actual interface GraphicsContext

/**
 * No-op measureComposable for Android.
 * Simple and vsync_emulation benchmark modes are not supported on Android.
 * Only the REAL mode (via Activity/BenchmarkRunner) works on Android.
 */
internal actual suspend fun measureComposable(
    name: String,
    warmupCount: Int,
    frameCount: Int,
    width: Int,
    height: Int,
    targetFps: Int,
    graphicsContext: GraphicsContext?,
    content: @Composable () -> Unit
): BenchmarkResult {
    error("measureComposable is not supported on Android. Use REAL mode instead.")
}
