package org.jetbrains.compose.resources

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.plural.PluralCategory
import org.jetbrains.compose.resources.plural.PluralRuleList
import org.jetbrains.compose.resources.vector.xmldom.Element
import org.jetbrains.compose.resources.vector.xmldom.NodeList

private val SimpleStringFormatRegex = Regex("""%(\d)\$[ds]""")

private fun String.replaceWithArgs(args: List<String>) = SimpleStringFormatRegex.replace(this) { matchResult ->
    args[matchResult.groupValues[1].toInt() - 1]
}

/**
 * Represents a string resource in the application.
 *
 * @param id The unique identifier of the resource.
 * @param key The key used to retrieve the string resource.
 * @param items The set of resource items associated with the string resource.
 */
@OptIn(InternalResourceApi::class)
@ExperimentalResourceApi
@Immutable
class StringResource
@InternalResourceApi constructor(id: String, val key: String, items: Set<ResourceItem>) : Resource(id, items)

/**
 * Represents a quantity string resource in the application.
 *
 * @param id The unique identifier of the resource.
 * @param key The key used to retrieve the string resource.
 * @param items The set of resource items associated with the string resource.
 */
@OptIn(InternalResourceApi::class)
@ExperimentalResourceApi
@Immutable
class PluralStringResource
@InternalResourceApi constructor(id: String, val key: String, items: Set<ResourceItem>) : Resource(id, items)

private sealed interface StringItem {
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
        val rawString = element.textContent.orEmpty()
        element.getAttribute("name") to StringItem.Value(handleSpecialCharacters(rawString))
    }
    val plurals = nodes.getElementsWithName("plurals").associate { pluralElement ->
        val items = pluralElement.childNodes.getElementsWithName("item").mapNotNull { element ->
            val pluralCategory = PluralCategory.fromString(
                element.getAttribute("quantity"),
            ) ?: return@mapNotNull null
            pluralCategory to element.textContent.orEmpty()
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

/**
 * Retrieves a string using the specified string resource.
 *
 * @param resource The string resource to be used.
 * @return The retrieved string resource.
 *
 * @throws IllegalArgumentException If the provided ID is not found in the resource file.
 */
@ExperimentalResourceApi
@Composable
fun stringResource(resource: StringResource): String {
    val resourceReader = LocalResourceReader.current
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
@ExperimentalResourceApi
suspend fun getString(resource: StringResource): String =
    loadString(resource, DefaultResourceReader, getResourceEnvironment())

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadString(
    resource: StringResource,
    resourceReader: ResourceReader,
    environment: ResourceEnvironment
): String {
    val path = resource.getPathByEnvironment(environment)
    val keyToValue = getParsedStrings(path, resourceReader)
    val item = keyToValue[resource.key] as? StringItem.Value
        ?: error("String ID=`${resource.key}` is not found!")
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
@ExperimentalResourceApi
@Composable
fun stringResource(resource: StringResource, vararg formatArgs: Any): String {
    val resourceReader = LocalResourceReader.current
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
@ExperimentalResourceApi
suspend fun getString(resource: StringResource, vararg formatArgs: Any): String = loadString(
    resource,
    formatArgs.map { it.toString() },
    DefaultResourceReader,
    getResourceEnvironment()
)

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadString(
    resource: StringResource,
    args: List<String>,
    resourceReader: ResourceReader,
    environment: ResourceEnvironment
): String {
    val str = loadString(resource, resourceReader, environment)
    return str.replaceWithArgs(args)
}

/**
 * Retrieves the string for the pluralization for the given quantity using the specified quantity string resource.
 *
 * @param resource The quantity string resource to be used.
 * @param quantity The quantity of the pluralization to use.
 * @return The retrieved string resource.
 *
 * @throws IllegalArgumentException If the provided ID or the pluralization is not found in the resource file.
 */
@ExperimentalResourceApi
@Composable
fun pluralStringResource(resource: PluralStringResource, quantity: Int): String {
    val resourceReader = LocalResourceReader.current
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
@ExperimentalResourceApi
suspend fun getPluralString(resource: PluralStringResource, quantity: Int): String =
    loadPluralString(resource, quantity, DefaultResourceReader, getResourceEnvironment())

@OptIn(InternalResourceApi::class, ExperimentalResourceApi::class)
private suspend fun loadPluralString(
    resource: PluralStringResource,
    quantity: Int,
    resourceReader: ResourceReader,
    environment: ResourceEnvironment
): String {
    val path = resource.getPathByEnvironment(environment)
    val keyToValue = getParsedStrings(path, resourceReader)
    val item = keyToValue[resource.key] as? StringItem.Plurals
        ?: error("Quantity string ID=`${resource.key}` is not found!")
    val pluralRuleList = PluralRuleList.getInstance(
        environment.language,
        environment.region,
    )
    val pluralCategory = pluralRuleList.getCategory(quantity)
    val str = item.items[pluralCategory]
        ?: error("String ID=`${resource.key}` does not have the pluralization $pluralCategory for quantity $quantity!")
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
@ExperimentalResourceApi
@Composable
fun pluralStringResource(resource: PluralStringResource, quantity: Int, vararg formatArgs: Any): String {
    val resourceReader = LocalResourceReader.current
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
@ExperimentalResourceApi
suspend fun getPluralString(resource: PluralStringResource, quantity: Int, vararg formatArgs: Any): String =
    loadPluralString(
        resource, quantity,
        formatArgs.map { it.toString() },
        DefaultResourceReader,
        getResourceEnvironment(),
    )

@OptIn(ExperimentalResourceApi::class)
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

/**
 * Retrieves a list of strings using the specified string array resource.
 *
 * @param resource The string resource to be used.
 * @return A list of strings representing the items in the string array.
 *
 * @throws IllegalStateException if the string array with the given ID is not found.
 */
@ExperimentalResourceApi
@Composable
fun stringArrayResource(resource: StringResource): List<String> {
    val resourceReader = LocalResourceReader.current
    val array by rememberResourceState(resource, { emptyList() }) { env ->
        loadStringArray(resource, resourceReader, env)
    }
    return array
}

/**
 * Loads a list of strings using the specified string array resource.
 *
 * @param resource The string resource to be used.
 * @return A list of strings representing the items in the string array.
 *
 * @throws IllegalStateException if the string array with the given ID is not found.
 */
@ExperimentalResourceApi
suspend fun getStringArray(resource: StringResource): List<String> =
    loadStringArray(resource, DefaultResourceReader, getResourceEnvironment())

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadStringArray(
    resource: StringResource,
    resourceReader: ResourceReader,
    environment: ResourceEnvironment
): List<String> {
    val path = resource.getPathByEnvironment(environment)
    val keyToValue = getParsedStrings(path, resourceReader)
    val item = keyToValue[resource.key] as? StringItem.Array
        ?: error("String array ID=`${resource.key}` is not found!")
    return item.items
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
