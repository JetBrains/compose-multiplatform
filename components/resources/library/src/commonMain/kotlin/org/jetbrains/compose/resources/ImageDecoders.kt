package org.jetbrains.compose.resources

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import org.jetbrains.compose.resources.vector.toImageVector

/**
 * Decodes a byte array of a Bitmap to an ImageBitmap. Supports JPEG, PNG, BMP, WEBP
 *
 * Different platforms can support additional formats.
 *
 * @return The converted ImageBitmap.
 */
fun ByteArray.decodeToImageBitmap(): ImageBitmap {
    val dumbDensity = 0 //any equal source and target density disable scaling here
    return this.toImageBitmap(dumbDensity, dumbDensity)
}

/**
 * Decodes a byte array of a vector XML file to an ImageVector.
 *
 * @param density density to apply during converting the source units to the [ImageVector] units.
 *
 * @return The converted ImageVector.
 */
fun ByteArray.decodeToImageVector(density: Density): ImageVector {
    return this.toXmlElement().toImageVector(density)
}
