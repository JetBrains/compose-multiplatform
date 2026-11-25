import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.PixelGeometry
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.jetbrains.skia.SurfaceProps

fun benchmarkSurface(context: DirectContext, target: BackendRenderTarget): Surface =
    Surface.makeFromBackendRenderTarget(
        context,
        target,
        SurfaceOrigin.TOP_LEFT,
        SurfaceColorFormat.BGRA_8888,
        ColorSpace.sRGB,
        SurfaceProps(pixelGeometry = PixelGeometry.UNKNOWN)
    ) ?: throw IllegalStateException("Can't create Surface")

fun Surface.submitBenchmarkFrame() {
    flushAndSubmit(false)
}
