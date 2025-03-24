package org.jetbrains.compose.resources

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density

/**
 * Decodes a byte array of an SVG file to a compose Painter.
 *
 * @param density density to apply during converting the source units to the [Painter] units.
 *
 * @return The converted Painter.
 */
fun ByteArray.decodeToSvgPainter(density: Density): Painter {
    return this.toSvgElement().toSvgPainter(density)
}