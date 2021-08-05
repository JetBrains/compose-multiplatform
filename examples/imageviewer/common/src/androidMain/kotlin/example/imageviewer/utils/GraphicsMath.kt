package example.imageviewer.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.compose.ui.layout.ContentScale
import kotlin.math.pow
import kotlin.math.roundToInt
import example.imageviewer.view.DragHandler

fun scaleBitmapAspectRatio(
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

fun getDisplayBounds(bitmap: Bitmap): Rect {

    val boundW: Float = displayWidth().toFloat()
    val boundH: Float = displayHeight().toFloat()

    val ratioX: Float = bitmap.width / boundW
    val ratioY: Float = bitmap.height / boundH
    val ratio: Float = if (ratioX > ratioY) ratioX else ratioY
    val resultW = (boundW * ratio)
    val resultH = (boundH * ratio)

    return Rect(0, 0, resultW.toInt(), resultH.toInt())
}

fun applyGrayScaleFilter(bitmap: Bitmap): Bitmap {

    val result: Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

    val canvas = Canvas(result)

    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(0f)

    val paint = Paint()
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)

    canvas.drawBitmap(result, 0f, 0f, paint)

    return result
}

fun applyPixelFilter(bitmap: Bitmap): Bitmap {

    var result: Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val w: Int = bitmap.width
    val h: Int = bitmap.height
    result = scaleBitmapAspectRatio(result, w / 20, h / 20)
    result = scaleBitmapAspectRatio(result, w, h)

    return result
}

fun applyBlurFilter(bitmap: Bitmap, context: Context): Bitmap {

    val result: Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

    val renderScript: RenderScript = RenderScript.create(context)

    val tmpIn: Allocation = Allocation.createFromBitmap(renderScript, bitmap)
    val tmpOut: Allocation = Allocation.createFromBitmap(renderScript, result)

    val theIntrinsic: ScriptIntrinsicBlur =
        ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))

    theIntrinsic.setRadius(25f)
    theIntrinsic.setInput(tmpIn)
    theIntrinsic.forEach(tmpOut)

    tmpOut.copyTo(result)

    return result
}

fun adjustImageScale(bitmap: Bitmap): ContentScale {
    val bitmapRatio = (10 * bitmap.width.toFloat() / bitmap.height).toInt()
    val displayRatio = (10 * displayWidth().toFloat() / displayHeight()).toInt()

    if (displayRatio > bitmapRatio) {
        return ContentScale.FillHeight
    }
    return ContentScale.FillWidth
}

fun toPx(dp: Int): Int {
    return (dp * Resources.getSystem().displayMetrics.density).toInt()
}

fun toDp(px: Int): Int {
    return (px / Resources.getSystem().displayMetrics.density).toInt()
}

fun displayWidth(): Int {
    return Resources.getSystem().displayMetrics.widthPixels
}

fun displayHeight(): Int {
    return Resources.getSystem().displayMetrics.heightPixels
}

fun cropBitmapByScale(bitmap: Bitmap, scale: Float, drag: DragHandler): Bitmap {
    val crop = cropBitmapByBounds(
        bitmap,
        getDisplayBounds(bitmap),
        scale,
        drag
    )
    return Bitmap.createBitmap(
        bitmap,
        crop.left,
        crop.top,
        crop.right - crop.left,
        crop.bottom - crop.top
    )
}

fun cropBitmapByBounds(
    bitmap: Bitmap,
    bounds: Rect,
    scaleFactor: Float,
    drag: DragHandler
): Rect {
    if (scaleFactor <= 1f)
        return Rect(0, 0, bitmap.width, bitmap.height)

    var scale = scaleFactor.toDouble().pow(1.4)

    var boundW = (bounds.width() / scale).roundToInt()
    var boundH = (bounds.height() / scale).roundToInt()

    scale *= displayWidth() / bounds.width().toDouble()

    val offsetX = drag.getAmount().x / scale
    val offsetY = drag.getAmount().y / scale

    if (boundW > bitmap.width) {
        boundW = bitmap.width
    }
    if (boundH > bitmap.height) {
        boundH = bitmap.height
    }

    val invisibleW = bitmap.width - boundW
    var leftOffset = (invisibleW / 2.0 - offsetX).roundToInt().toFloat()

    if (leftOffset > invisibleW) {
        leftOffset = invisibleW.toFloat()
        drag.getAmount().x = -((invisibleW / 2.0) * scale).roundToInt().toFloat()
    }
    if (leftOffset < 0) {
        drag.getAmount().x = ((invisibleW / 2.0) * scale).roundToInt().toFloat()
        leftOffset = 0f
    }

    val invisibleH = bitmap.height - boundH
    var topOffset = (invisibleH / 2 - offsetY).roundToInt().toFloat()

    if (topOffset > invisibleH) {
        topOffset = invisibleH.toFloat()
        drag.getAmount().y = -((invisibleH / 2.0) * scale).roundToInt().toFloat()
    }
    if (topOffset < 0) {
        drag.getAmount().y = ((invisibleH / 2.0) * scale).roundToInt().toFloat()
        topOffset = 0f
    }

    return Rect(
        leftOffset.toInt(),
        topOffset.toInt(),
        (leftOffset + boundW).toInt(),
        (topOffset + boundH).toInt()
    )
}