package example.imageviewer.utils

import org.jetbrains.skia.*

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

	var result = scaleBitmapAspectRatio(bitmap, width / 20, height / 20)
	result = scaleBitmapAspectRatio(result, width, height)

	return result
}

fun applyBlurFilter(bitmap: Bitmap): Bitmap {
	val result = Bitmap().apply {
		allocN32Pixels(bitmap.width, bitmap.height)
	}
	val blur = Paint().apply {
		imageFilter = ImageFilter.makeBlur(10f, 10f, FilterTileMode.CLAMP)
	}

	val canvas = Canvas(result)
	canvas.saveLayer(null, blur)
	canvas.drawImageRect(Image.makeFromBitmap(bitmap), bitmap.bounds.toRect())
	canvas.restore()
	canvas.readPixels(result, 0, 0)
	canvas.close()

	return result
}