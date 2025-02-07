import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.PixelGeometry
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.jetbrains.skia.SurfaceProps
import org.jetbrains.skiko.ExperimentalSkikoApi
import org.jetbrains.skiko.graphicapi.DirectXOffscreenContext
import org.jetbrains.skiko.hostOs

@OptIn(ExperimentalSkikoApi::class)
fun graphicsContext(): GraphicsContext? = when {
    hostOs.isWindows -> DirectXGraphicsContext()
    else -> {
        println("Unsupported desktop host OS: $hostOs. Using non-GPU graphic context")
        null
    }
}

@OptIn(ExperimentalSkikoApi::class)
class DirectXGraphicsContext() : GraphicsContext {
    // Note: we don't close `context` and `texture` after using,
    // because it is created once in the main function
    private val context = DirectXOffscreenContext()
    private var texture: DirectXOffscreenContext.Texture? = null

    override fun surface(width: Int, height: Int): Surface {
        texture?.close()
        texture = context.Texture(width, height)
        return Surface.makeFromBackendRenderTarget(
            context.directContext,
            texture!!.backendRenderTarget,
            SurfaceOrigin.TOP_LEFT,
            SurfaceColorFormat.BGRA_8888,
            ColorSpace.sRGB,
            SurfaceProps(pixelGeometry = PixelGeometry.UNKNOWN)
        ) ?: throw IllegalStateException("Can't create Surface")
    }

    override suspend fun awaitGPUCompletion() {
        texture?.waitForCompletion()
    }
}
