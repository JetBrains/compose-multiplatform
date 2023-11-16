package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
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
 * @param id The ID of the string resource to retrieve.
 * @return The retrieved string resource.
 *
 * @throws IllegalArgumentException If the provided ID is not found in the resource file.
 */
@ExperimentalResourceApi
@Composable
fun getString(id: ResourceId): String {
    val resourceReader = LocalResourceReader.current
    val str by rememberState(id, { "" }) { loadString(id, resourceReader) }
    return str
}

/**
 * Loads a string resource using the provided ID.
 *
 * @param id The ID of the string resource to load.
 * @return The loaded string resource.
 *
 * @throws IllegalArgumentException If the provided ID is not found in the resource file.
 */
@ExperimentalResourceApi
suspend fun loadString(id: ResourceId): String = loadString(id, DefaultResourceReader)

private suspend fun loadString(id: ResourceId, resourceReader: ResourceReader): String {
    val path = getPathById(id, resourceReader)
    val nameToValue = getParsedStrings(path, resourceReader)
    val item = nameToValue[id.stringKey] as? StringItem.Value
        ?: error("String ID=`${id.stringKey}` is not found!")
    return item.text
}

/**
 * Retrieves a formatted string resource using the provided ID and arguments.
 *
 * @param id The ID of the string resource to retrieve.
 * @param formatArgs The arguments to be inserted into the formatted string.
 * @return The formatted string resource.
 *
 * @throws IllegalArgumentException If the provided ID is not found in the resource file.
 */
@ExperimentalResourceApi
@Composable
fun getString(id: ResourceId, vararg formatArgs: Any): String {
    val resourceReader = LocalResourceReader.current
    val args = formatArgs.map { it.toString() }
    val str by rememberState(id, { "" }) { loadString(id, args, resourceReader) }
    return str
}

/**
 * Loads a formatted string resource using the provided ID and arguments.
 *
 * @param id The ID of the string resource to load.
 * @param formatArgs The arguments to be inserted into the formatted string.
 * @return The formatted string resource.
 *
 * @throws IllegalArgumentException If the provided ID is not found in the resource file.
 */
@ExperimentalResourceApi
suspend fun loadString(id: ResourceId, vararg formatArgs: Any): String = loadString(
    id,
    formatArgs.map { it.toString() },
    DefaultResourceReader
)

private suspend fun loadString(id: ResourceId, args: List<String>, resourceReader: ResourceReader): String {
    val str = loadString(id, resourceReader)
    return SimpleStringFormatRegex.replace(str) { matchResult ->
        args[matchResult.groupValues[1].toInt() - 1]
    }
}

/**
 * Retrieves a list of strings from a string array resource.
 *
 * @param id The ID of the string array resource.
 * @return A list of strings representing the items in the string array.
 *
 * @throws IllegalStateException if the string array with the given ID is not found.
 */
@ExperimentalResourceApi
@Composable
fun getStringArray(id: ResourceId): List<String> {
    val resourceReader = LocalResourceReader.current
    val array by rememberState(id, { emptyList() }) { loadStringArray(id, resourceReader) }
    return array
}

/**
 * Loads a string array from a resource file.
 *
 * @param id The ID of the string array resource.
 * @return A list of strings representing the items in the string array.
 *
 * @throws IllegalStateException if the string array with the given ID is not found.
 */
@ExperimentalResourceApi
suspend fun loadStringArray(id: ResourceId): List<String> = loadStringArray(id, DefaultResourceReader)

private suspend fun loadStringArray(id: ResourceId, resourceReader: ResourceReader): List<String> {
    val path = getPathById(id, resourceReader)
    val nameToValue = getParsedStrings(path, resourceReader)
    val item = nameToValue[id.stringKey] as? StringItem.Array
        ?: error("String array ID=`${id.stringKey}` is not found!")
    return item.items
}

private fun NodeList.getElementsWithName(name: String): List<Element> =
    List(length) { item(it) }
        .filterIsInstance<Element>()
        .filter { it.localName == name }