package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.*

/**
 * Represents a font resource.
 *
 * @param id The identifier of the font resource.
 * @param items The set of resource items associated with the font resource.
 *
 * @see Resource
 */
@Immutable
class FontResource
@InternalResourceApi constructor(id: String, items: Set<ResourceItem>) : Resource(id, items)

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
@Deprecated(
    message = "Use the new Font function with variationSettings instead.",
    level = DeprecationLevel.HIDDEN
)
@Composable
expect fun Font(
    resource: FontResource,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): Font

/**
 * Creates a font using the specified font resource, weight, and style.
 *
 * @param resource The font resource to be used.
 * @param weight The weight of the font. Default value is [FontWeight.Normal].
 * @param style The style of the font. Default value is [FontStyle.Normal].
 * @param variationSettings Custom variation settings for the font, with a default value derived from the specified [weight] and [style].
 *
 * @return The created [Font] object.
 *
 * @throws NotFoundException if the specified resource ID is not found.
 */
@Composable
expect fun Font(
    resource: FontResource,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
    variationSettings: FontVariation.Settings = FontVariation.Settings(weight, style)
): Font

/**
 * Retrieves the byte array of the font resource.
 *
 * @param environment The resource environment, which can be obtained from [rememberResourceEnvironment] or [getSystemResourceEnvironment].
 * @param resource The font resource.
 * @return The byte array representing the font resource.
 */
suspend fun getFontResourceBytes(
    environment: ResourceEnvironment,
    resource: FontResource
): ByteArray {
    val resourceItem = resource.getResourceItemByEnvironment(environment)
    return DefaultResourceReader.read(resourceItem.path)
}