package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.vector.toImageVector
import org.jetbrains.compose.resources.vector.xmldom.Element

/**
 * Represents a drawable resource.
 *
 * @param id The unique identifier of the drawable resource.
 * @param items The set of resource items associated with the image resource.
 */
@Immutable
class DrawableResource
@InternalResourceApi constructor(id: String, items: Set<ResourceItem>) : Resource(id, items)

/**
 * Retrieves a [Painter] using the specified drawable resource.
 * Automatically select a type of the Painter depending on the file extension.
 *
 * @param resource The drawable resource to be used.
 * @return The [Painter] loaded from the resource.
 */
@Composable
fun painterResource(resource: DrawableResource): Painter {
    val environment = LocalComposeEnvironment.current.rememberEnvironment()
    val filePath = remember(resource, environment) { resource.getResourceItemByEnvironment(environment).path }
    if (filePath.endsWith(".xml", true)) {
        return rememberVectorPainter(vectorResource(resource))
    } else if (filePath.endsWith(".svg", true)) {
        return svgPainter(resource)
    } else {
        return BitmapPainter(imageResource(resource))
    }
}

private val emptyImageBitmap: ImageBitmap by lazy { ImageBitmap(1, 1) }

internal val ImageBitmap.isEmptyPlaceholder: Boolean
    get() = this == emptyImageBitmap

/**
 * Retrieves an ImageBitmap using the specified drawable resource.
 *
 * @param resource The drawable resource to be used.
 * @return The ImageBitmap loaded from the resource.
 */
@Composable
fun imageResource(resource: DrawableResource): ImageBitmap {
    val resourceReader = LocalResourceReader.currentOrPreview
    val resourceEnvironment = rememberResourceEnvironment()
    val imageBitmap by rememberResourceState(
        resource, resourceReader, resourceEnvironment, { emptyImageBitmap }
    ) { env ->
        val item = resource.getResourceItemByEnvironment(env)
        val resourceDensityQualifier = item.qualifiers.firstOrNull { it is DensityQualifier } as? DensityQualifier
        val resourceDensity = resourceDensityQualifier?.dpi ?: DensityQualifier.MDPI.dpi
        val screenDensity = resourceEnvironment.density.dpi
        val path = item.path
        val cached = loadImage(path, "$path-${screenDensity}dpi", resourceReader) {
            ImageCache.Bitmap(it.toImageBitmap(resourceDensity, screenDensity))
        } as ImageCache.Bitmap
        cached.bitmap
    }
    return imageBitmap
}

private val emptyImageVector: ImageVector by lazy {
    ImageVector.Builder("emptyImageVector", 1.dp, 1.dp, 1f, 1f).build()
}

internal val ImageVector.isEmptyPlaceholder: Boolean
    get() = this == emptyImageVector

/**
 * Retrieves an ImageVector using the specified drawable resource.
 *
 * @param resource The drawable resource to be used.
 * @return The ImageVector loaded from the resource.
 */
@Composable
fun vectorResource(resource: DrawableResource): ImageVector {
    val resourceReader = LocalResourceReader.currentOrPreview
    val density = LocalDensity.current
    val imageVector by rememberResourceState(resource, resourceReader, density, { emptyImageVector }) { env ->
        val path = resource.getResourceItemByEnvironment(env).path
        val cached = loadImage(path, path, resourceReader) {
            ImageCache.Vector(it.toXmlElement().toImageVector(density))
        } as ImageCache.Vector
        cached.vector
    }
    return imageVector
}

internal expect class SvgElement

internal expect fun SvgElement.toSvgPainter(density: Density): Painter

private val emptySvgPainter: Painter by lazy { BitmapPainter(emptyImageBitmap) }

@Composable
private fun svgPainter(resource: DrawableResource): Painter {
    val resourceReader = LocalResourceReader.currentOrPreview
    val density = LocalDensity.current
    val svgPainter by rememberResourceState(resource, resourceReader, density, { emptySvgPainter }) { env ->
        val path = resource.getResourceItemByEnvironment(env).path
        val cached = loadImage(path, path, resourceReader) {
            ImageCache.Svg(it.toSvgElement().toSvgPainter(density))
        } as ImageCache.Svg
        cached.painter
    }
    return svgPainter
}

/**
 * Retrieves the byte array of the drawable resource.
 *
 * @param environment The resource environment, which can be obtained from [rememberResourceEnvironment] or [getSystemResourceEnvironment].
 * @param resource The drawable resource.
 * @return The byte array representing the drawable resource.
 */
suspend fun getDrawableResourceBytes(
    environment: ResourceEnvironment,
    resource: DrawableResource
): ByteArray {
    val resourceItem = resource.getResourceItemByEnvironment(environment)
    return DefaultResourceReader.read(resourceItem.path)
}

internal expect fun ByteArray.toImageBitmap(resourceDensity: Int, targetDensity: Int): ImageBitmap
internal expect fun ByteArray.toXmlElement(): Element
internal expect fun ByteArray.toSvgElement(): SvgElement

private sealed interface ImageCache {
    class Bitmap(val bitmap: ImageBitmap) : ImageCache
    class Vector(val vector: ImageVector) : ImageCache
    class Svg(val painter: Painter) : ImageCache
}

private val imageCache = AsyncCache<String, ImageCache>()

private suspend fun loadImage(
    path: String,
    cacheKey: String,
    resourceReader: ResourceReader,
    decode: (ByteArray) -> ImageCache
): ImageCache = imageCache.getOrLoad(cacheKey) { decode(resourceReader.read(path)) }
