package org.jetbrains.compose.resources

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density

internal actual fun ByteArray.toImageBitmap(): ImageBitmap =
    BitmapFactory.decodeByteArray(this, 0, size).asImageBitmap()

internal actual class SvgElement

internal actual fun ByteArray.toSvgElement(): SvgElement {
    error("Android platform doesn't support SVG format.")
}

internal actual fun SvgElement.toSvgPainter(density: Density): Painter {
    error("Android platform doesn't support SVG format.")
}