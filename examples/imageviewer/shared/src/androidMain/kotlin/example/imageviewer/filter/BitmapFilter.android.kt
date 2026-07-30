package example.imageviewer.filter

import android.content.Context
import android.graphics.*
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.scale
import kotlin.math.max

actual fun grayScaleFilter(bitmap: ImageBitmap, context: PlatformContext): ImageBitmap {
    return applyGrayScaleFilter(bitmap.asAndroidBitmap()).asImageBitmap()
}

actual fun pixelFilter(bitmap: ImageBitmap, context: PlatformContext): ImageBitmap {
    return applyPixelFilter(bitmap.asAndroidBitmap()).asImageBitmap()
}

actual fun blurFilter(bitmap: ImageBitmap, context: PlatformContext): ImageBitmap {
    return applyBlurFilter(bitmap.asAndroidBitmap()).asImageBitmap()
}

actual class PlatformContext(val androidContext: Context)

@Composable
actual fun getPlatformContext(): PlatformContext = PlatformContext(LocalContext.current)

private const val BLUR_RADIUS = 20f

private fun applyBlurFilter(bitmap: Bitmap): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        blurWithRenderEffect(bitmap, BLUR_RADIUS)
    } else {
        blurByDownscaling(bitmap)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun blurWithRenderEffect(bitmap: Bitmap, radius: Float): Bitmap {
    val renderNode = RenderNode("BlurFilter")
    val hardwareRenderer = HardwareRenderer()
    try {
        val usage = HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
        ImageReader.newInstance(bitmap.width, bitmap.height, PixelFormat.RGBA_8888, 1, usage).use { imageReader ->
            hardwareRenderer.setSurface(imageReader.surface)
            hardwareRenderer.setContentRoot(renderNode)
            renderNode.setPosition(0, 0, bitmap.width, bitmap.height)
            renderNode.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP))

            renderNode.beginRecording().drawBitmap(bitmap, 0f, 0f, null)
            renderNode.endRecording()

            hardwareRenderer.createRenderRequest()
                .setWaitForPresent(true)
                .syncAndDraw()

            val image = imageReader.acquireNextImage() ?: return bitmap.copy(Bitmap.Config.ARGB_8888, true)
            return image.use {
                it.hardwareBuffer?.use { buffer -> Bitmap.wrapHardwareBuffer(buffer, null) }
                    ?.copy(Bitmap.Config.ARGB_8888, false)
                    ?: bitmap.copy(Bitmap.Config.ARGB_8888, true)
            }
        }
    } finally {
        hardwareRenderer.destroy()
    }
}

private fun blurByDownscaling(bitmap: Bitmap): Bitmap {
    val downscaled = bitmap.scale(max(1, bitmap.width / 20), max(1, bitmap.height / 20))
    return downscaled.scale(bitmap.width, bitmap.height)
}

private fun applyGrayScaleFilter(bitmap: Bitmap): Bitmap {

    val result: Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

    val canvas = Canvas(result)

    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(0f)

    val paint = Paint()
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)

    canvas.drawBitmap(result, 0f, 0f, paint)

    return result
}

private fun applyPixelFilter(bitmap: Bitmap): Bitmap {

    var result: Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val w: Int = bitmap.width
    val h: Int = bitmap.height
    result = scaleBitmapAspectRatio(result, w / 12, h / 12)
    result = scaleBitmapAspectRatio(result, w, h)

    return result
}

private fun scaleBitmapAspectRatio(
    bitmap: Bitmap,
    width: Int,
    height: Int,
    filter: Boolean = false
): Bitmap {
    val boundW: Float = width.toFloat()
    val boundH: Float = height.toFloat()

    val ratioX: Float = boundW / bitmap.width
    val ratioY: Float = boundH / bitmap.height
    val ratio: Float = if (ratioX < ratioY) ratioX else ratioY

    val resultH = (bitmap.height * ratio).toInt()
    val resultW = (bitmap.width * ratio).toInt()

    return bitmap.scale(resultW, resultH, filter)
}
