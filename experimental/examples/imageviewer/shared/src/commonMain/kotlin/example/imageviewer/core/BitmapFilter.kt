package example.imageviewer.core

expect class CommonBitmap
expect fun createEmptyBitmap():CommonBitmap

interface BitmapFilter {
    fun apply(bitmap: CommonBitmap) : CommonBitmap
}
