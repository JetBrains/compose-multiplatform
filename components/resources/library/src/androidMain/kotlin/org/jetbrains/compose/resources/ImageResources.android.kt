package org.jetbrains.compose.resources

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density

internal actual fun ByteArray.toImageBitmap(resourceDensity: Int, targetDensity: Int): ImageBitmap {
    val options = BitmapFactory.Options().apply {
        //https://youtrack.jetbrains.com/issue/CMP-5657
        //android only downscales drawables. If there is only low dpi resource then use it as is (not upscale)
        if (resourceDensity > targetDensity) {
            inDensity = resourceDensity
            inTargetDensity = targetDensity
        }
    }
    return BitmapFactory.decodeByteArray(this, 0, size, options).asImageBitmap()
}

internal actual class SvgElement

internal actual fun ByteArray.toSvgElement(): SvgElement {
    error("Android platform doesn't support SVG format.")
}

internal actual fun SvgElement.toSvgPainter(density: Density): Painter {
    error("Android platform doesn't support SVG format.")
}