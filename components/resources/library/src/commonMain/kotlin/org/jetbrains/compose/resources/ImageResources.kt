package org.jetbrains.compose.resources

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.vector.toImageVector
import org.jetbrains.compose.resources.vector.xmldom.Element

/**
 * Represents a drawable resource.
 *
 * @param id The unique identifier of the drawable resource.
 * @param items The set of resource items associated with the image resource.
 */
@Immutable
class DrawableResource(id: String, items: Set<ResourceItem>) : Resource(id, items)

/**
 * Creates an [DrawableResource] object with the specified path.
 *
 * @param path The path of the drawable resource.
 * @return An [DrawableResource] object.
 */
fun DrawableResource(path: String): DrawableResource = DrawableResource(
    id = "DrawableResource:$path",
    items = setOf(ResourceItem(emptySet(), path))
)

/**
 * Retrieves a [Painter] using the specified drawable resource.
 * Automatically select a type of the Painter depending on the file extension.
 *
 * @param resource The drawable resource to be used.
 * @return The [Painter] loaded from the resource.
 */
@ExperimentalResourceApi
@Composable
fun painterResource(resource: DrawableResource): Painter {
    val environment = LocalComposeEnvironment.current.rememberEnvironment()
    val filePath = remember(resource, environment) { resource.getPathByEnvironment(environment) }
    val isXml = filePath.endsWith(".xml", true)
    if (isXml) {
        return rememberVectorPainter(vectorResource(resource))
    } else {
        return BitmapPainter(imageResource(resource))
    }
}

private val emptyImageBitmap: ImageBitmap by lazy { ImageBitmap(1, 1) }

/**
 * Retrieves an ImageBitmap using the specified drawable resource.
 *
 * @param resource The drawable resource to be used.
 * @return The ImageBitmap loaded from the resource.
 */
@ExperimentalResourceApi
@Composable
fun imageResource(resource: DrawableResource): ImageBitmap {
    val resourceReader = LocalResourceReader.current
    val imageBitmap by rememberResourceState(resource, { emptyImageBitmap }) { env ->
        val path = resource.getPathByEnvironment(env)
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
 * Retrieves an ImageVector using the specified drawable resource.
 *
 * @param resource The drawable resource to be used.
 * @return The ImageVector loaded from the resource.
 */
@ExperimentalResourceApi
@Composable
fun vectorResource(resource: DrawableResource): ImageVector {
    val resourceReader = LocalResourceReader.current
    val density = LocalDensity.current
    val imageVector by rememberResourceState(resource, { emptyImageVector }) { env ->
        val path = resource.getPathByEnvironment(env)
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
