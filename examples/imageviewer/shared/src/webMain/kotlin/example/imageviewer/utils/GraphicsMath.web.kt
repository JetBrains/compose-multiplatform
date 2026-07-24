package example.imageviewer.utils

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint

fun scaleBitmapAspectRatio(
	bitmap: Bitmap,
	width: Int,
	height: Int
): Bitmap {
	val boundWidth = width.toFloat()
	val boundHeight = height.toFloat()

	val ratioX = boundWidth / bitmap.width
	val ratioY = boundHeight / bitmap.height
	val ratio = if (ratioX < ratioY) ratioX else ratioY

	val resultWidth = (bitmap.width * ratio).toInt()
	val resultHeight = (bitmap.height * ratio).toInt()

	val result = Bitmap().apply {
		allocN32Pixels(resultWidth, resultHeight)
	}
	val canvas = Canvas(result)
	canvas.drawImageRect(Image.makeFromBitmap(bitmap), result.bounds.toRect())
	canvas.readPixels(result, 0, 0)
	canvas.close()

	return result
}

fun applyGrayScaleFilter(bitmap: Bitmap): Bitmap {
	val imageInfo = ImageInfo(
		width = bitmap.width,
		height = bitmap.height,
		colorInfo = ColorInfo(ColorType.GRAY_8, ColorAlphaType.PREMUL, null)
	)
	val result = Bitmap().apply {
		allocPixels(imageInfo)
	}

	val canvas = Canvas(result)
	canvas.drawImageRect(Image.makeFromBitmap(bitmap), bitmap.bounds.toRect())
	canvas.readPixels(result, 0, 0)
	canvas.close()

	return result
}

fun applyPixelFilter(bitmap: Bitmap): Bitmap {
	val width = bitmap.width
	val height = bitmap.height

	var result = scaleBitmapAspectRatio(bitmap, width / 4, height / 4)
	result = scaleBitmapAspectRatio(result, width, height)

	return result
}

fun applyBlurFilter(bitmap: Bitmap): Bitmap {
	val result = Bitmap().apply {
		allocN32Pixels(bitmap.width, bitmap.height)
	}
	val blur = Paint().apply {
		imageFilter = ImageFilter.makeBlur(3f, 3f, FilterTileMode.CLAMP)
	}

	val canvas = Canvas(result)
	canvas.saveLayer(null, blur)
	canvas.drawImageRect(Image.makeFromBitmap(bitmap), bitmap.bounds.toRect())
	canvas.restore()
	canvas.readPixels(result, 0, 0)
	canvas.close()

	return result
}