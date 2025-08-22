package org.jetbrains.compose.resources

import org.jetbrains.compose.resources.plural.PluralCategory
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val SimpleStringFormatRegex = Regex("""%(\d+)\$(0?)(\d+)?([ds])""")

internal fun String.replaceWithArgs(args: List<String>) = SimpleStringFormatRegex.replace(this) { matchResult ->
    val index = matchResult.groupValues[1].toInt() - 1
    val flag = matchResult.groupValues[2]
    val width = matchResult.groupValues[3].takeIf { it.isNotEmpty() }?.toInt() ?: 0
    val type = matchResult.groupValues[4]

    val value = args[index]

    when (type) {
        "d" -> {
            when(flag) {
                "0" -> {
                    value.padStart(width, '0')
                }
                else -> {
                    value.padStart(width)
                }
            }
        }
        "s" -> value.padStart(width)
        else -> value
    }
}

internal sealed interface StringItem {
    data class Value(val text: String) : StringItem
    data class Plurals(val items: Map<PluralCategory, String>) : StringItem
    data class Array(val items: List<String>) : StringItem
}

private val stringItemsCache = AsyncCache<String, StringItem>()

internal suspend fun getStringItem(
    resourceItem: ResourceItem,
    resourceReader: ResourceReader
): StringItem = stringItemsCache.getOrLoad(
    key = "${resourceItem.path}/${resourceItem.offset}-${resourceItem.size}"
) {
    val record = resourceReader.readPart(
        resourceItem.path,
        resourceItem.offset,
        resourceItem.size
    ).decodeToString()
    val recordItems = record.split('|')
    val recordType = recordItems.first()
    val recordData = recordItems.last()
    when (recordType) {
        "plurals" -> recordData.decodeAsPlural()
        "string-array" -> recordData.decodeAsArray()
        else -> recordData.decodeAsString()
    }
}

@OptIn(ExperimentalEncodingApi::class)
private fun String.decodeAsString(): StringItem.Value = StringItem.Value(
    Base64.decode(this).decodeToString()
)

@OptIn(ExperimentalEncodingApi::class)
private fun String.decodeAsArray(): StringItem.Array = StringItem.Array(
    split(",").map { item ->
        Base64.decode(item).decodeToString()
    }
)

@OptIn(ExperimentalEncodingApi::class)
private fun String.decodeAsPlural(): StringItem.Plurals = StringItem.Plurals(
    split(",").associate { item ->
        val category = item.substringBefore(':')
        val valueBase64 = item.substringAfter(':')
        PluralCategory.fromString(category)!! to Base64.decode(valueBase64).decodeToString()
    }
)
