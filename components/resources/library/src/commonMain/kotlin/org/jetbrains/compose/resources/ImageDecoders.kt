package org.jetbrains.compose.resources

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import org.jetbrains.compose.resources.vector.toImageVector

/**
 * Decodes a byte array of a vector XML file to an ImageVector.
 *
 * @return The converted ImageVector.
 */
@ExperimentalResourceApi
fun ByteArray.decodeToImageVector(density: Density): ImageVector {
    return this.toXmlElement().toImageVector(density)
}
