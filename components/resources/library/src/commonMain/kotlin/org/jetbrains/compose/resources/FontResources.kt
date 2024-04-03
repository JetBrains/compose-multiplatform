package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.*

/**
 * Represents a font resource.
 *
 * @param id The identifier of the font resource.
 * @param items The set of resource items associated with the font resource.
 *
 * @see Resource
 */
@ExperimentalResourceApi
@Immutable
class FontResource
@InternalResourceApi constructor(id: String, items: Set<ResourceItem>): Resource(id, items)

/**
 * Creates an [FontResource] object with the specified path.
 *
 * @param path The path to the font resource file.
 * @return A new [FontResource] object.
 */
@ExperimentalResourceApi
fun FontResource(path: String): FontResource = FontResource(
    id = "FontResource:$path",
    items = setOf(ResourceItem(emptySet(), path, -1, -1))
)

/**
 * Creates a font using the specified font resource, weight, and style.
 *
 * @param resource The font resource to be used.
 * @param weight The weight of the font. Default value is [FontWeight.Normal].
 * @param style The style of the font. Default value is [FontStyle.Normal].
 *
 * @return The created [Font] object.
 *
 * @throws NotFoundException if the specified resource ID is not found.
 */
@Composable
@ExperimentalResourceApi
expect fun Font(
    resource: FontResource,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): Font