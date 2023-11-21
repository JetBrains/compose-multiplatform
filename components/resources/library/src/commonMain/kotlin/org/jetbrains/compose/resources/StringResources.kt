package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.vector.xmldom.Element
import org.jetbrains.compose.resources.vector.xmldom.NodeList

private val SimpleStringFormatRegex = Regex("""%(\d)\$[ds]""")

/**
 * Represents a string resource in the application.
 *
 * @param id The unique identifier of the resource.
 * @param key The key used to retrieve the string resource.
 * @param items The set of resource items associated with the string resource.
 */
@Immutable
class StringResource(id: String, val key: String, items: Set<ResourceItem>): Resource(id, items)

private sealed interface StringItem {
    data class Value(val text: String) : StringItem
    data class Array(val items: List<String>) : StringItem
}

private val stringsCacheMutex = Mutex()
private val parsedStringsCache = mutableMapOf<String, Deferred<Map<String, StringItem>>>()

//@TestOnly
internal fun dropStringsCache() {
    parsedStringsCache.clear()
}

private suspend fun getParsedStrings(
    path: String,
    resourceReader: ResourceReader
): Map<String, StringItem> = coroutineScope {
    val deferred = stringsCacheMutex.withLock {
        parsedStringsCache.getOrPut(path) {
            //LAZY - to free the mutex lock as fast as possible
            async(start = CoroutineStart.LAZY) {
                parseStringXml(path, resourceReader)
            }
        }
    }
    deferred.await()
}

private suspend fun parseStringXml(path: String, resourceReader: ResourceReader): Map<String, StringItem> {
    val nodes = resourceReader.read(path).toXmlElement().childNodes
    val strings = nodes.getElementsWithName("string").associate { element ->
        element.getAttribute("name") to StringItem.Value(element.textContent.orEmpty())
    }
    val arrays = nodes.getElementsWithName("string-array").associate { arrayElement ->
        val items = arrayElement.childNodes.getElementsWithName("item").map { element ->
            element.textContent.orEmpty()
        }
        arrayElement.getAttribute("name") to StringItem.Array(items)
    }
    return strings + arrays
}

/**
 * Retrieves a string resource using the provided ID.
 *
 * @param resource The string resource to be used.
 * @return The retrieved string resource.
 *
 * @throws IllegalArgumentException If the provided ID is not found in the resource file.
 */
@ExperimentalResourceApi
@Composable
fun getString(resource: StringResource): String {
    val resourceReader = LocalResourceReader.current
    val str by rememberState(resource, { "" }) { loadString(resource, resourceReader) }
    return str
}

/**
 * Loads a string resource using the provided ID.
 *
 * @param resource The string resource to be used.
 * @return The loaded string resource.
 *
 * @throws IllegalArgumentException If the provided ID is not found in the resource file.
 */
@ExperimentalResourceApi
suspend fun loadString(resource: StringResource): String = loadString(resource, DefaultResourceReader)

private suspend fun loadString(resource: StringResource, resourceReader: ResourceReader): String {
    val path = resource.getPathByEnvironment()
    val keyToValue = getParsedStrings(path, resourceReader)
    val item = keyToValue[resource.key] as? StringItem.Value
        ?: error("String ID=`${resource.id}` is not found!")
    return item.text
}

/**
 * Retrieves a formatted string resource using the provided ID and arguments.
 *
 * @param resource The string resource to be used.
 * @param formatArgs The arguments to be inserted into the formatted string.
 * @return The formatted string resource.
 *
 * @throws IllegalArgumentException If the provided ID is not found in the resource file.
 */
@ExperimentalResourceApi
@Composable
fun getString(resource: StringResource, vararg formatArgs: Any): String {
    val resourceReader = LocalResourceReader.current
    val args = formatArgs.map { it.toString() }
    val str by rememberState(resource, { "" }) { loadString(resource, args, resourceReader) }
    return str
}

/**
 * Loads a formatted string resource using the provided ID and arguments.
 *
 * @param resource The string resource to be used.
 * @param formatArgs The arguments to be inserted into the formatted string.
 * @return The formatted string resource.
 *
 * @throws IllegalArgumentException If the provided ID is not found in the resource file.
 */
@ExperimentalResourceApi
suspend fun loadString(resource: StringResource, vararg formatArgs: Any): String = loadString(
    resource,
    formatArgs.map { it.toString() },
    DefaultResourceReader
)

private suspend fun loadString(resource: StringResource, args: List<String>, resourceReader: ResourceReader): String {
    val str = loadString(resource, resourceReader)
    return SimpleStringFormatRegex.replace(str) { matchResult ->
        args[matchResult.groupValues[1].toInt() - 1]
    }
}

/**
 * Retrieves a list of strings from a string array resource.
 *
 * @param resource The string resource to be used.
 * @return A list of strings representing the items in the string array.
 *
 * @throws IllegalStateException if the string array with the given ID is not found.
 */
@ExperimentalResourceApi
@Composable
fun getStringArray(resource: StringResource): List<String> {
    val resourceReader = LocalResourceReader.current
    val array by rememberState(resource, { emptyList() }) { loadStringArray(resource, resourceReader) }
    return array
}

/**
 * Loads a string array from a resource file.
 *
 * @param resource The string resource to be used.
 * @return A list of strings representing the items in the string array.
 *
 * @throws IllegalStateException if the string array with the given ID is not found.
 */
@ExperimentalResourceApi
suspend fun loadStringArray(resource: StringResource): List<String> = loadStringArray(resource, DefaultResourceReader)

private suspend fun loadStringArray(resource: StringResource, resourceReader: ResourceReader): List<String> {
    val path = resource.getPathByEnvironment()
    val keyToValue = getParsedStrings(path, resourceReader)
    val item = keyToValue[resource.key] as? StringItem.Array
        ?: error("String array ID=`${resource.key}` is not found!")
    return item.items
}

private fun NodeList.getElementsWithName(name: String): List<Element> =
    List(length) { item(it) }
        .filterIsInstance<Element>()
        .filter { it.localName == name }