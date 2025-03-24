package org.jetbrains.compose.resources

import androidx.compose.runtime.*

/**
 * Represents a string resource in the application.
 *
 * @param id The unique identifier of the resource.
 * @param key The key used to retrieve the string resource.
 * @param items The set of resource items associated with the string resource.
 */
@Immutable
class StringResource
@InternalResourceApi constructor(id: String, val key: String, items: Set<ResourceItem>) : Resource(id, items)

/**
 * Retrieves a string using the specified string resource.
 *
 * @param resource The string resource to be used.
 * @return The retrieved string resource.
 *
 * @throws IllegalArgumentException If the provided ID is not found in the resource file.
 */
@Composable
fun stringResource(resource: StringResource): String {
    val resourceReader = LocalResourceReader.currentOrPreview
    val str by rememberResourceState(resource, { "" }) { env ->
        loadString(resource, resourceReader, env)
    }
    return str
}

/**
 * Loads a string using the specified string resource.
 *
 * @param resource The string resource to be used.
 * @return The loaded string resource.
 *
 * @throws IllegalArgumentException If the provided ID is not found in the resource file.
 */
suspend fun getString(resource: StringResource): String =
    loadString(resource, DefaultResourceReader, getSystemResourceEnvironment())

/**
 * Loads a string using the specified string resource.
 *
 * @param environment The resource environment.
 * @param resource The string resource to be used.
 * @return The loaded string resource.
 *
 * @throws IllegalArgumentException If the provided ID is not found in the resource file.
 */
suspend fun getString(environment: ResourceEnvironment, resource: StringResource): String =
    loadString(resource, DefaultResourceReader, environment)

private suspend fun loadString(
    resource: StringResource,
    resourceReader: ResourceReader,
    environment: ResourceEnvironment
): String {
    val resourceItem = resource.getResourceItemByEnvironment(environment)
    val item = getStringItem(resourceItem, resourceReader) as StringItem.Value
    return item.text
}

/**
 * Retrieves a formatted string using the specified string resource and arguments.
 *
 * @param resource The string resource to be used.
 * @param formatArgs The arguments to be inserted into the formatted string.
 * @return The formatted string resource.
 *
 * @throws IllegalArgumentException If the provided ID is not found in the resource file.
 */
@Composable
fun stringResource(resource: StringResource, vararg formatArgs: Any): String {
    val resourceReader = LocalResourceReader.currentOrPreview
    val args = formatArgs.map { it.toString() }
    val str by rememberResourceState(resource, args, { "" }) { env ->
        loadString(resource, args, resourceReader, env)
    }
    return str
}

/**
 * Loads a formatted string using the specified string resource and arguments.
 *
 * @param resource The string resource to be used.
 * @param formatArgs The arguments to be inserted into the formatted string.
 * @return The formatted string resource.
 *
 * @throws IllegalArgumentException If the provided ID is not found in the resource file.
 */
suspend fun getString(resource: StringResource, vararg formatArgs: Any): String = loadString(
    resource,
    formatArgs.map { it.toString() },
    DefaultResourceReader,
    getSystemResourceEnvironment()
)

/**
 * Loads a formatted string using the specified string resource and arguments.
 *
 * @param environment The resource environment.
 * @param resource The string resource to be used.
 * @param formatArgs The arguments to be inserted into the formatted string.
 * @return The formatted string resource.
 *
 * @throws IllegalArgumentException If the provided ID is not found in the resource file.
 */
suspend fun getString(
    environment: ResourceEnvironment,
    resource: StringResource,
    vararg formatArgs: Any
): String = loadString(
    resource,
    formatArgs.map { it.toString() },
    DefaultResourceReader,
    environment
)

private suspend fun loadString(
    resource: StringResource,
    args: List<String>,
    resourceReader: ResourceReader,
    environment: ResourceEnvironment
): String {
    val str = loadString(resource, resourceReader, environment)
    return str.replaceWithArgs(args)
}
