package org.jetbrains.compose.resources

import androidx.compose.runtime.*
import org.jetbrains.compose.resources.plural.PluralCategory
import org.jetbrains.compose.resources.plural.PluralRuleList

/**
 * Represents a quantity string resource in the application.
 *
 * @param id The unique identifier of the resource.
 * @param key The key used to retrieve the string resource.
 * @param items The set of resource items associated with the string resource.
 */
@Immutable
class PluralStringResource
@InternalResourceApi constructor(id: String, val key: String, items: Set<ResourceItem>) : Resource(id, items)

/**
 * Retrieves the string for the pluralization for the given quantity using the specified quantity string resource.
 *
 * @param resource The quantity string resource to be used.
 * @param quantity The quantity of the pluralization to use.
 * @return The retrieved string resource.
 *
 * @throws IllegalArgumentException If the provided ID or the pluralization is not found in the resource file.
 */
@Composable
fun pluralStringResource(resource: PluralStringResource, quantity: Int): String {
    val resourceReader = LocalResourceReader.currentOrPreview
    val pluralStr by rememberResourceState(resource, quantity, { "" }) { env ->
        loadPluralString(resource, quantity, resourceReader, env)
    }
    return pluralStr
}

/**
 * Loads a string using the specified string resource.
 *
 * @param resource The string resource to be used.
 * @param quantity The quantity of the pluralization to use.
 * @return The loaded string resource.
 *
 * @throws IllegalArgumentException If the provided ID or the pluralization is not found in the resource file.
 */
suspend fun getPluralString(resource: PluralStringResource, quantity: Int): String =
    loadPluralString(resource, quantity, DefaultResourceReader, getSystemResourceEnvironment())

/**
 * Loads a string using the specified string resource.
 *
 * @param environment The resource environment.
 * @param resource The string resource to be used.
 * @param quantity The quantity of the pluralization to use.
 * @return The loaded string resource.
 *
 * @throws IllegalArgumentException If the provided ID or the pluralization is not found in the resource file.
 */
suspend fun getPluralString(
    environment: ResourceEnvironment,
    resource: PluralStringResource,
    quantity: Int
): String = loadPluralString(resource, quantity, DefaultResourceReader, environment)

private suspend fun loadPluralString(
    resource: PluralStringResource,
    quantity: Int,
    resourceReader: ResourceReader,
    environment: ResourceEnvironment
): String {
    val resourceItem = resource.getResourceItemByEnvironment(environment)
    val item = getStringItem(resourceItem, resourceReader) as StringItem.Plurals
    val pluralRuleList = PluralRuleList.getInstance(
        environment.language,
        environment.region,
    )
    val pluralCategory = pluralRuleList.getCategory(quantity)
    val str = item.items[pluralCategory]
        ?: item.items[PluralCategory.OTHER]
        ?: error("Quantity string ID=`${resource.key}` does not have the pluralization $pluralCategory for quantity $quantity!")
    return str
}

/**
 * Retrieves the string for the pluralization for the given quantity using the specified quantity string resource.
 *
 * @param resource The quantity string resource to be used.
 * @param quantity The quantity of the pluralization to use.
 * @param formatArgs The arguments to be inserted into the formatted string.
 * @return The retrieved string resource.
 *
 * @throws IllegalArgumentException If the provided ID or the pluralization is not found in the resource file.
 */
@Composable
fun pluralStringResource(resource: PluralStringResource, quantity: Int, vararg formatArgs: Any): String {
    val resourceReader = LocalResourceReader.currentOrPreview
    val args = formatArgs.map { it.toString() }
    val pluralStr by rememberResourceState(resource, quantity, args, { "" }) { env ->
        loadPluralString(resource, quantity, args, resourceReader, env)
    }
    return pluralStr
}

/**
 * Loads a string using the specified string resource.
 *
 * @param resource The string resource to be used.
 * @param quantity The quantity of the pluralization to use.
 * @param formatArgs The arguments to be inserted into the formatted string.
 * @return The loaded string resource.
 *
 * @throws IllegalArgumentException If the provided ID or the pluralization is not found in the resource file.
 */
suspend fun getPluralString(resource: PluralStringResource, quantity: Int, vararg formatArgs: Any): String =
    loadPluralString(
        resource, quantity,
        formatArgs.map { it.toString() },
        DefaultResourceReader,
        getSystemResourceEnvironment(),
    )

/**
 * Loads a string using the specified string resource.
 *
 * @param environment The resource environment.
 * @param resource The string resource to be used.
 * @param quantity The quantity of the pluralization to use.
 * @param formatArgs The arguments to be inserted into the formatted string.
 * @return The loaded string resource.
 *
 * @throws IllegalArgumentException If the provided ID or the pluralization is not found in the resource file.
 */
suspend fun getPluralString(
    environment: ResourceEnvironment,
    resource: PluralStringResource,
    quantity: Int,
    vararg formatArgs: Any
): String = loadPluralString(
    resource, quantity,
    formatArgs.map { it.toString() },
    DefaultResourceReader,
    environment
)

private suspend fun loadPluralString(
    resource: PluralStringResource,
    quantity: Int,
    args: List<String>,
    resourceReader: ResourceReader,
    environment: ResourceEnvironment
): String {
    val str = loadPluralString(resource, quantity, resourceReader, environment)
    return str.replaceWithArgs(args)
}
