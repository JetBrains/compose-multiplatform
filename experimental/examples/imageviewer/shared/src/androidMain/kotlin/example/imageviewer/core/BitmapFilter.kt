package example.imageviewer.core

actual typealias CommonBitmap = android.graphics.Bitmap

actual fun createEmptyBitmap() = CommonBitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
