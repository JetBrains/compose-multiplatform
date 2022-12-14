package example.imageviewer.core

import android.graphics.Bitmap

interface BitmapFilter {
    fun apply(bitmap: Bitmap) : Bitmap
}