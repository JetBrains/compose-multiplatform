package example.imageviewer.utils

import androidx.compose.ui.window.WindowSize
import androidx.compose.ui.unit.dp
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import java.awt.image.BufferedImageOp
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import kotlin.math.pow
import kotlin.math.roundToInt
import example.imageviewer.view.DragHandler

fun scaleBitmapAspectRatio(
    bitmap: BufferedImage,
    width: Int,
    height: Int
): BufferedImage {
    val boundW: Float = width.toFloat()
    val boundH: Float = height.toFloat()

    val ratioX: Float = boundW / bitmap.width
    val ratioY: Float = boundH / bitmap.height
    val ratio: Float = if (ratioX < ratioY) ratioX else ratioY

    val resultH = (bitmap.height * ratio).toInt()
    val resultW = (bitmap.width * ratio).toInt()

    val result = BufferedImage(resultW, resultH, BufferedImage.TYPE_INT_ARGB)
    val graphics = result.createGraphics()
    graphics.drawImage(bitmap, 0, 0, resultW, resultH, null)
    graphics.dispose()

    return result
}

fun getDisplayBounds(bitmap: BufferedImage, windowSize: WindowSize): Rectangle {

    val boundW: Float = windowSize.width.value.toFloat()
    val boundH: Float = windowSize.height.value.toFloat()

    val ratioX: Float = bitmap.width / boundW
    val ratioY: Float = bitmap.height / boundH

    val ratio: Float = if (ratioX > ratioY) ratioX else ratioY

    val resultW = (boundW * ratio)
    val resultH = (boundH * ratio)

    return Rectangle(0, 0, resultW.toInt(), resultH.toInt())
}

fun applyGrayScaleFilter(bitmap: BufferedImage): BufferedImage {

    val result = BufferedImage(
        bitmap.getWidth(),
        bitmap.getHeight(),
        BufferedImage.TYPE_BYTE_GRAY)

    val graphics = result.getGraphics()
    graphics.drawImage(bitmap, 0, 0, null)
    graphics.dispose()

    return result
}

fun applyPixelFilter(bitmap: BufferedImage): BufferedImage {

    val w: Int = bitmap.width
    val h: Int = bitmap.height

    var result = scaleBitmapAspectRatio(bitmap, w / 20, h / 20)
    result = scaleBitmapAspectRatio(result, w, h)

    return result
}

fun applyBlurFilter(bitmap: BufferedImage): BufferedImage {

    var result = BufferedImage(bitmap.getWidth(), bitmap.getHeight(), bitmap.type)

    val graphics = result.getGraphics()
    graphics.drawImage(bitmap, 0, 0, null)
    graphics.dispose()

    val radius = 11
    val size = 11
    val weight: Float = 1.0f / (size * size)
    val matrix = FloatArray(size * size)

    for (i in 0..matrix.size - 1) {
        matrix[i] = weight
    }

    val kernel = Kernel(radius, size, matrix)
    val op = ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null)
    result = op.filter(result, null)

    return result.getSubimage(
        radius,
        radius,
        result.width - radius * 2,
        result.height - radius * 2
    )
}

fun toByteArray(bitmap: BufferedImage) : ByteArray {
    val baos = ByteArrayOutputStream()
    ImageIO.write(bitmap, "png", baos)
    return baos.toByteArray()
}

fun cropImage(bitmap: BufferedImage, crop: Rectangle) : BufferedImage {
    return bitmap.getSubimage(crop.x, crop.y, crop.width, crop.height)
}

fun cropBitmapByScale(
    bitmap: BufferedImage,
    size: WindowSize,
    scale: Float,
    drag: DragHandler
): BufferedImage {
    val crop = cropBitmapByBounds(
        bitmap,
        getDisplayBounds(bitmap, size),
        size,
        scale,
        drag
    )
    return cropImage(
        bitmap,
        Rectangle(crop.x, crop.y, crop.width - crop.x, crop.height - crop.y)
    )
}

fun cropBitmapByBounds(
    bitmap: BufferedImage,
    bounds: Rectangle,
    size: WindowSize,
    scaleFactor: Float,
    drag: DragHandler
): Rectangle {

    if (scaleFactor <= 1f) {
        return Rectangle(0, 0, bitmap.width, bitmap.height)
    }

    var scale = scaleFactor.toDouble().pow(1.4)

    var boundW = (bounds.width / scale).roundToInt()
    var boundH = (bounds.height / scale).roundToInt()

    scale *= size.width.value / bounds.width.toDouble()

    val offsetX = drag.getAmount().x / scale
    val offsetY = drag.getAmount().y / scale

    if (boundW > bitmap.width) {
        boundW = bitmap.width
    }
    if (boundH > bitmap.height) {
        boundH = bitmap.height
    }

    val invisibleW = bitmap.width - boundW
    var leftOffset = (invisibleW / 2.0 - offsetX).roundToInt()

    if (leftOffset > invisibleW) {
        leftOffset = invisibleW
        drag.getAmount().x = -((invisibleW / 2.0) * scale).roundToInt().toFloat()
    }
    if (leftOffset < 0) {
        drag.getAmount().x = ((invisibleW / 2.0) * scale).roundToInt().toFloat()
        leftOffset = 0
    }

    val invisibleH = bitmap.height - boundH
    var topOffset = (invisibleH / 2 - offsetY).roundToInt()

    if (topOffset > invisibleH) {
        topOffset = invisibleH
        drag.getAmount().y = -((invisibleH / 2.0) * scale).roundToInt().toFloat()
    }
    if (topOffset < 0) {
        drag.getAmount().y = ((invisibleH / 2.0) * scale).roundToInt().toFloat()
        topOffset = 0
    }

    return Rectangle(leftOffset, topOffset, leftOffset + boundW, topOffset + boundH)
}

fun getPreferredWindowSize(desiredWidth: Int, desiredHeight: Int): WindowSize {
    val screenSize: Dimension = Toolkit.getDefaultToolkit().screenSize
    val preferredWidth: Int = (screenSize.width * 0.8f).toInt()
    val preferredHeight: Int = (screenSize.height * 0.8f).toInt()
    val width: Int = if (desiredWidth < preferredWidth) desiredWidth else preferredWidth
    val height: Int = if (desiredHeight < preferredHeight) desiredHeight else preferredHeight
    return WindowSize(width.dp, height.dp)
}
