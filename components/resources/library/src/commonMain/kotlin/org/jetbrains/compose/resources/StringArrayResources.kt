package org.jetbrains.compose.resources

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.plural.PluralCategory
import org.jetbrains.compose.resources.plural.PluralRuleList
import org.jetbrains.compose.resources.vector.xmldom.Element
import org.jetbrains.compose.resources.vector.xmldom.NodeList

/**
 * Represents a string array resource in the application.
 *
 * @param id The unique identifier of the resource.
 * @param key The key used to retrieve the string array resource.
 * @param items The set of resource items associated with the string array resource.
 */
@Immutable
class StringArrayResource
@InternalResourceApi constructor(id: String, val key: String, items: Set<ResourceItem>) : Resource(id, items)

/**
 * Retrieves a list of strings using the specified string array resource.
 *
 * @param resource The string array resource to be used.
 * @return A list of strings representing the items in the string array.
 *
 * @throws IllegalStateException if the string array with the given ID is not found.
 */
@Composable
fun stringArrayResource(resource: StringArrayResource): List<String> {
    val resourceReader = LocalResourceReader.currentOrPreview
    val array by rememberResourceState(resource, { emptyList() }) { env ->
        loadStringArray(resource, resourceReader, env)
    }
    return array
}

/**
 * Loads a list of strings using the specified string array resource.
 *
 * @param resource The string array resource to be used.
 * @return A list of strings representing the items in the string array.
 *
 * @throws IllegalStateException if the string array with the given ID is not found.
 */
suspend fun getStringArray(resource: StringArrayResource): List<String> =
    loadStringArray(resource, DefaultResourceReader, getSystemResourceEnvironment())

/**
 * Loads a list of strings using the specified string array resource.
 *
 * @param environment The resource environment.
 * @param resource The string array resource to be used.
 * @return A list of strings representing the items in the string array.
 *
 * @throws IllegalStateException if the string array with the given ID is not found.
 */
suspend fun getStringArray(
    environment: ResourceEnvironment,
    resource: StringArrayResource
): List<String> = loadStringArray(resource, DefaultResourceReader, environment)

private suspend fun loadStringArray(
    resource: StringArrayResource,
    resourceReader: ResourceReader,
    environment: ResourceEnvironment
): List<String> {
    val resourceItem = resource.getResourceItemByEnvironment(environment)
    val item = getStringItem(resourceItem, resourceReader) as StringItem.Array
    return item.items
}
