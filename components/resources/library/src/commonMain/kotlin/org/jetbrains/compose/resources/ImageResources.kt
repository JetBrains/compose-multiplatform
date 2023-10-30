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
 * Reads image files and provides the Painter abstraction
 */
@ExperimentalResourceApi
@Composable
fun rememberPainter(id: ResourceId): Painter {
    val filePath by rememberFilePath(id)
    val isXml = filePath.endsWith(".xml", true)
    if (isXml) {
        return rememberVectorPainter(rememberImageVector(id))
    } else {
        return BitmapPainter(rememberImageBitmap(id))
    }
}

private val emptyImageBitmap: ImageBitmap by lazy { ImageBitmap(1, 1) }

@ExperimentalResourceApi
@Composable
fun rememberImageBitmap(id: ResourceId): ImageBitmap {
    val fileContent by rememberState(ByteArray(0)) { readBytes(getPathById(id)) }

    //it is fallback only for JS async loading
    if (fileContent.isEmpty()) return emptyImageBitmap

    return remember(id) { fileContent.toImageBitmap() }
}

private val emptyImageVector: ImageVector by lazy {
    ImageVector.Builder("emptyImageVector", 1.dp, 1.dp, 1f, 1f).build()
}

@ExperimentalResourceApi
@Composable
fun rememberImageVector(id: ResourceId): ImageVector {
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
