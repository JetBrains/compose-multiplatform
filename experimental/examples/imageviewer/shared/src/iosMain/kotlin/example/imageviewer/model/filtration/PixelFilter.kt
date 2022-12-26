package example.imageviewer.model.filtration

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import example.imageviewer.core.BitmapFilter
import example.imageviewer.utils.applyPixelFilter

class PixelFilter : BitmapFilter {
	override fun apply(bitmap: ImageBitmap): ImageBitmap {
		return applyPixelFilter(bitmap.asSkiaBitmap()).asComposeImageBitmap()
	}
}