package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    val resourceReader = LocalResourceReader.current
    val imageBitmap by rememberState(id, { emptyImageBitmap }) {
        val path = getPathById(id)
        val cached = loadImage(path, resourceReader) {
            ImageCache.Bitmap(it.toImageBitmap())
        } as ImageCache.Bitmap
        cached.bitmap
    }
    return imageBitmap
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
    val resourceReader = LocalResourceReader.current
    val density = LocalDensity.current
    val imageVector by rememberState(id, { emptyImageVector }) {
        val path = getPathById(id)
        val cached = loadImage(path, resourceReader) {
            ImageCache.Vector(it.toXmlElement().toImageVector(density))
        } as ImageCache.Vector
        cached.vector
    }
    return imageVector
}

internal expect fun ByteArray.toImageBitmap(): ImageBitmap
internal expect fun ByteArray.toXmlElement(): Element

private sealed interface ImageCache {
    class Bitmap(val bitmap: ImageBitmap) : ImageCache
    class Vector(val vector: ImageVector) : ImageCache
}

private val imageCacheMutex = Mutex()
private val imageCache = mutableMapOf<String, Deferred<ImageCache>>()

//@TestOnly
internal fun dropImageCache() {
    imageCache.clear()
}

private suspend fun loadImage(
    path: String,
    resourceReader: ResourceReader,
    decode: (ByteArray) -> ImageCache
): ImageCache = coroutineScope {
    val deferred = imageCacheMutex.withLock {
        imageCache.getOrPut(path) {
            //LAZY - to free the mutex lock as fast as possible
            async(start = CoroutineStart.LAZY) {
                decode(resourceReader.read(path))
            }
        }
    }
    deferred.await()
}
