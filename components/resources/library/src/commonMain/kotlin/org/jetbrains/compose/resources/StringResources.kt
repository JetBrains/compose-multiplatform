package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.jetbrains.compose.resources.vector.xmldom.Element
import org.jetbrains.compose.resources.vector.xmldom.NodeList

private const val STRINGS_XML = "strings.xml" //todo
private val SimpleStringFormatRegex = Regex("""%(\d)\$[ds]""")

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
    val str by rememberState("") { loadString(id) }
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
suspend fun loadString(id: ResourceId): String {
    val bytes = readBytes(getPathById(STRINGS_XML))
    val nodes = bytes.toXmlElement().childNodes

    val nameToValue = nodes.getElementsWithName("string").associate { element ->
        element.getAttribute("name") to element.textContent.orEmpty()
    }

    return nameToValue[id] ?: error("String ID=$id is not found!")
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
    val str = getString(id)
    val args = formatArgs.map { it.toString() }
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
    val array by rememberState(emptyList()) { loadStringArray(id) }
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
suspend fun loadStringArray(id: ResourceId): List<String> {
    val bytes = readBytes(getPathById(STRINGS_XML))
    val nodes = bytes.toXmlElement().childNodes
    val arrayElement = nodes.getElementsWithName("string-array").firstOrNull { element ->
        element.getAttribute("name") == id
    } ?: error("String array ID=$id is not found!")

    val items = arrayElement.childNodes.getElementsWithName("item").map { element ->
        element.textContent.orEmpty()
    }

    return items
}

private fun NodeList.getElementsWithName(name: String): List<Element> =
    List(length) { item(it) }
        .filterIsInstance<Element>()
        .filter { it.localName == name }