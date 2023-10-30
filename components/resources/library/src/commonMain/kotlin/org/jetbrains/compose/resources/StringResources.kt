package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import org.jetbrains.compose.resources.vector.xmldom.Element
import org.jetbrains.compose.resources.vector.xmldom.NodeList

private const val STRINGS_XML = "strings.xml" //todo

@ExperimentalResourceApi
@Composable
fun getString(id: ResourceId): String {
    val str by rememberState("") { loadString(id) }
    return str
}

@ExperimentalResourceApi
suspend fun loadString(id: ResourceId): String {
    val bytes = readBytes(getPathById(STRINGS_XML))
    val nodes = bytes.toXmlElement().childNodes

    val nameToValue = nodes.getElementsWithName("string").associate { element ->
        element.getAttribute("name") to element.textContent.orEmpty()
    }

    return nameToValue[id] ?: error("String ID=$id is not found!")
}

private fun NodeList.getElementsWithName(name: String): List<Element> =
    List(length) { item(it) }
        .filterIsInstance<Element>()
        .filter { it.localName == name }

private val SimpleStringFormatRegex = Regex("""%(\d)\$[ds]""")

@ExperimentalResourceApi
@Composable
fun getString(id: ResourceId, vararg formatArgs: Any): String {
    val str = getString(id)
    val args = formatArgs.map { it.toString() }
    return SimpleStringFormatRegex.replace(str) { matchResult ->
        args[matchResult.groupValues[1].toInt() - 1]
    }
}

@ExperimentalResourceApi
@Composable
fun getStringArray(id: ResourceId): List<String> {
    val array by rememberState(emptyList()) { loadStringArray(id) }
    return array
}

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

private fun getPluralString(id: ResourceId, count: Int): List<String> = TODO()
private fun getPluralString(id: ResourceId, count: Int, vararg formatArgs: Any): List<String> = TODO()