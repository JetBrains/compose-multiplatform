import kotlin.time.TimeSource

actual val mainTime: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()

actual val isSvgSupported: Boolean = false

actual fun getProcessStartTime(): TimeSource.Monotonic.ValueTimeMark? {
    return null
}