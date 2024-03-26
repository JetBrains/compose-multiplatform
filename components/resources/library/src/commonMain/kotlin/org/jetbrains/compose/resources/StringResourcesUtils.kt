package org.jetbrains.compose.resources

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.plural.PluralCategory
import org.jetbrains.compose.resources.vector.xmldom.Element
import org.jetbrains.compose.resources.vector.xmldom.NodeList

private val SimpleStringFormatRegex = Regex("""%(\d)\$[ds]""")

internal fun String.replaceWithArgs(args: List<String>) = SimpleStringFormatRegex.replace(this) { matchResult ->
    args[matchResult.groupValues[1].toInt() - 1]
}

internal sealed interface StringItem {
    data class Value(val text: String) : StringItem
    data class Plurals(val items: Map<PluralCategory, String>) : StringItem
    data class Array(val items: List<String>) : StringItem
}

private val stringsCacheMutex = Mutex()
private val parsedStringsCache = mutableMapOf<String, Deferred<Map<String, StringItem>>>()

//@TestOnly
internal fun dropStringsCache() {
    parsedStringsCache.clear()
}

internal suspend fun getParsedStrings(
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
        val rawString = element.textContent.orEmpty()
        element.getAttribute("name") to StringItem.Value(handleSpecialCharacters(rawString))
    }
    val plurals = nodes.getElementsWithName("plurals").associate { pluralElement ->
        val items = pluralElement.childNodes.getElementsWithName("item").mapNotNull { element ->
            val pluralCategory = PluralCategory.fromString(
                element.getAttribute("quantity"),
            ) ?: return@mapNotNull null
            pluralCategory to handleSpecialCharacters(element.textContent.orEmpty())
        }
        pluralElement.getAttribute("name") to StringItem.Plurals(items.toMap())
    }
    val arrays = nodes.getElementsWithName("string-array").associate { arrayElement ->
        val items = arrayElement.childNodes.getElementsWithName("item").map { element ->
            val rawString = element.textContent.orEmpty()
            handleSpecialCharacters(rawString)
        }
        arrayElement.getAttribute("name") to StringItem.Array(items)
    }
    return strings + plurals + arrays
}

private fun NodeList.getElementsWithName(name: String): List<Element> =
    List(length) { item(it) }
        .filterIsInstance<Element>()
        .filter { it.localName == name }

//https://developer.android.com/guide/topics/resources/string-resource#escaping_quotes
/**
 * Replaces
 *
 * '\n' -> new line
 *
 * '\t' -> tab
 *
 * '\uXXXX' -> unicode symbol
 *
 * '\\' -> '\'
 *
 * @param string The input string to handle.
 * @return The string with special characters replaced according to the logic.
 */
internal fun handleSpecialCharacters(string: String): String {
    val unicodeNewLineTabRegex = Regex("""\\u[a-fA-F\d]{4}|\\n|\\t""")
    val doubleSlashRegex = Regex("""\\\\""")
    val doubleSlashIndexes = doubleSlashRegex.findAll(string).map { it.range.first }
    val handledString = unicodeNewLineTabRegex.replace(string) { matchResult ->
        if (doubleSlashIndexes.contains(matchResult.range.first - 1)) matchResult.value
        else when (matchResult.value) {
            "\\n" -> "\n"
            "\\t" -> "\t"
            else -> matchResult.value.substring(2).toInt(16).toChar().toString()
        }
    }.replace("""\\""", """\""")
    return handledString
}
