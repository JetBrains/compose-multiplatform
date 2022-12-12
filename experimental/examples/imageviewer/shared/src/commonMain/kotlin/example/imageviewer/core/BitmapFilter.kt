package example.imageviewer.core

expect class Bitmap

interface BitmapFilter {
    fun apply(bitmap: Bitmap) : Bitmap
}
