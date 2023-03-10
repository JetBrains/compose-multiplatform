package example.imageviewer.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur

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
    result = scaleBitmapAspectRatio(result, w / 4, h / 4)
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

    theIntrinsic.setRadius(3f)
    theIntrinsic.setInput(tmpIn)
    theIntrinsic.forEach(tmpOut)

    tmpOut.copyTo(result)

    return result
}
