package example.imageviewer.filter

import android.content.Context
import android.graphics.*
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

actual fun grayScaleFilter(bitmap: ImageBitmap, context: PlatformContext): ImageBitmap {
    return applyGrayScaleFilter(bitmap.asAndroidBitmap()).asImageBitmap()
}

actual fun pixelFilter(bitmap: ImageBitmap, context: PlatformContext): ImageBitmap {
    return applyPixelFilter(bitmap.asAndroidBitmap()).asImageBitmap()
}

actual fun blurFilter(bitmap: ImageBitmap, context: PlatformContext): ImageBitmap {
    return applyBlurFilter(bitmap.asAndroidBitmap(), context.androidContext).asImageBitmap()
}

actual class PlatformContext(val androidContext: Context)

@Composable
actual fun getPlatformContext(): PlatformContext = PlatformContext(LocalContext.current)


private fun applyBlurFilter(bitmap: Bitmap, context: Context): Bitmap {

    val result: Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

    val renderScript: RenderScript = RenderScript.create(context)

    val tmpIn: Allocation = Allocation.createFromBitmap(renderScript, bitmap)
    val tmpOut: Allocation = Allocation.createFromBitmap(renderScript, result)

    val theIntrinsic: ScriptIntrinsicBlur =
        ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))

    theIntrinsic.setRadius(15f)
    theIntrinsic.setInput(tmpIn)
    theIntrinsic.forEach(tmpOut)

    tmpOut.copyTo(result)

    return result
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

    return Bitmap.createScaledBitmap(bitmap, resultW, resultH, filter)
}

