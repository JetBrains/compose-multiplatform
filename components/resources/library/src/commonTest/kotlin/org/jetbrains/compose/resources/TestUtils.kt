package org.jetbrains.compose.resources

@OptIn(InternalResourceApi::class, ExperimentalResourceApi::class)
internal fun TestStringResource(key: String) = StringResource(
    "STRING:$key",
    key,
    setOf(ResourceItem(emptySet(), "strings.xml"))
)

@OptIn(InternalResourceApi::class, ExperimentalResourceApi::class)
internal fun TestQuantityStringResource(key: String) = QuantityStringResource(
    "PLURALS:$key",
    key,
    setOf(ResourceItem(emptySet(), "strings.xml"))
)

fun parsePluralSamples(samples: String): List<Int> {
    return samples.split(',').flatMap {
        val range = it.trim()
        when {
            range.isEmpty() -> emptyList()
            range in arrayOf("…", "...") -> emptyList()
            // ignore numbers in compact exponent format
            range.contains('c') || range.contains('e') -> emptyList()
            range.contains('~') -> {
                val (start, endInclusive) = range.split('~')
                return@flatMap (start.toInt()..endInclusive.toInt()).toList()
            }

            else -> listOf(range.toInt())
        }
    }
}