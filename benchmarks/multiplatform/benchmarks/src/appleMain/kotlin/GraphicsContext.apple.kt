import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.objcPtr
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.PixelGeometry
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.jetbrains.skia.SurfaceProps
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLPixelFormatBGRA8Unorm
import platform.Metal.MTLTextureDescriptor
import platform.Metal.MTLTextureType2D
import platform.Metal.MTLTextureUsageRenderTarget
import platform.Metal.MTLTextureUsageShaderRead
import platform.Metal.MTLTextureUsageShaderWrite
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
fun graphicsContext() = object : GraphicsContext {
    private val device = MTLCreateSystemDefaultDevice() ?: throw IllegalStateException("Can't create MTLDevice")
    private val commandQueue = device.newCommandQueue() ?: throw IllegalStateException("Can't create MTLCommandQueue")
    private val directContext = DirectContext.makeMetal(device.objcPtr(), commandQueue.objcPtr())

    override fun surface(width: Int, height: Int): Surface {
        val descriptor = MTLTextureDescriptor()
        descriptor.width = width.toULong()
        descriptor.height = height.toULong()
        descriptor.usage = MTLTextureUsageShaderRead or MTLTextureUsageShaderWrite or MTLTextureUsageRenderTarget
        descriptor.textureType = MTLTextureType2D
        descriptor.pixelFormat = MTLPixelFormatBGRA8Unorm
        descriptor.mipmapLevelCount = 1UL

        val texture = device.newTextureWithDescriptor(descriptor) ?: throw IllegalStateException("Can't create MTLTexture")

        val renderTarget = BackendRenderTarget.makeMetal(width, height, texture.objcPtr())

        return Surface.makeFromBackendRenderTarget(
            directContext,
            renderTarget,
            SurfaceOrigin.TOP_LEFT,
            SurfaceColorFormat.BGRA_8888,
            ColorSpace.sRGB,
            SurfaceProps(pixelGeometry = PixelGeometry.UNKNOWN)
        ) ?: throw IllegalStateException("Can't create Surface")
    }

    override suspend fun awaitGPUCompletion() {
        val commandBuffer = commandQueue.commandBuffer() ?: return

        suspendCoroutine { continuation ->
            commandBuffer.addCompletedHandler {
                continuation.resume(Unit)
            }

            commandBuffer.commit()
        }
    }
}




