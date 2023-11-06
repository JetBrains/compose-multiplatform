package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.vector.toImageVector
import org.jetbrains.compose.resources.vector.xmldom.Element


/**
 * Retrieves a [Painter] for the given [ResourceId].
 * Automatically select a type of the Painter depending on the file extension.
 *
 * @param id The ID of the resource to retrieve the [Painter] from.
 * @return The [Painter] loaded from the resource.
 */
@ExperimentalResourceApi
@Composable
fun painterResource(id: ResourceId): Painter {
    val filePath by rememberFilePath(id)
    val isXml = filePath.endsWith(".xml", true)
    if (isXml) {
        return rememberVectorPainter(vectorResource(id))
    } else {
        return BitmapPainter(imageResource(id))
    }
}

private val emptyImageBitmap: ImageBitmap by lazy { ImageBitmap(1, 1) }

/**
 * Retrieves an ImageBitmap for the given resource ID.
 *
 * @param id The ID of the resource to load the ImageBitmap from.
 * @return The ImageBitmap loaded from the resource.
 */
@ExperimentalResourceApi
@Composable
fun imageResource(id: ResourceId): ImageBitmap {
    val fileContent by rememberState(ByteArray(0)) { readBytes(getPathById(id)) }

    //it is fallback only for JS async loading
    if (fileContent.isEmpty()) return emptyImageBitmap

    return remember(id) { fileContent.toImageBitmap() }
}

private val emptyImageVector: ImageVector by lazy {
    ImageVector.Builder("emptyImageVector", 1.dp, 1.dp, 1f, 1f).build()
}

/**
 * Retrieves an ImageVector for the given resource ID.
 *
 * @param id The ID of the resource to load the ImageVector from.
 * @return The ImageVector loaded from the resource.
 */
@ExperimentalResourceApi
@Composable
fun vectorResource(id: ResourceId): ImageVector {
    val fileContent by rememberState(ByteArray(0)) { readBytes(getPathById(id)) }

    //it is fallback only for JS async loading
    if (fileContent.isEmpty()) return emptyImageVector

    val density = LocalDensity.current
    return remember(id, density) {
        val element = fileContent.toXmlElement()
        element.toImageVector(density)
    }
}

internal expect fun ByteArray.toImageBitmap(): ImageBitmap
internal expect fun ByteArray.toXmlElement(): Element
