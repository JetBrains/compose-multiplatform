package org.jetbrains.compose.resources

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.svg.SVGDOM

internal actual fun ByteArray.toImageBitmap(): ImageBitmap =
    Image.makeFromEncoded(this).toComposeImageBitmap()

internal actual class SvgElement(val svgdom: SVGDOM)

internal actual fun ByteArray.toSvgElement(): SvgElement =
    SvgElement(SVGDOM(Data.makeFromBytes(this)))

internal actual fun SvgElement.toSvgPainter(density: Density): Painter =
    SvgPainter(svgdom, density)