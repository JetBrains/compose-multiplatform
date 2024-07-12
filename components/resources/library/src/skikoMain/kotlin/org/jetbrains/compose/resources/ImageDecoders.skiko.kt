package org.jetbrains.compose.resources

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density

/**
 * Decodes a byte array of an SVG file to a compose Painter.
 *
 * @return The converted Painter.
 */
@ExperimentalResourceApi
fun ByteArray.decodeToSvgPainter(density: Density): Painter {
    return this.toSvgElement().toSvgPainter(density)
}