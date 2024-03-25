package org.jetbrains.compose.resources

import org.jetbrains.compose.resources.plural.PluralCategory
import org.jetbrains.compose.resources.plural.PluralRule
import org.jetbrains.compose.resources.plural.PluralRuleList

@OptIn(InternalResourceApi::class, ExperimentalResourceApi::class)
internal fun TestStringResource(key: String) = StringResource(
    "STRING:$key",
    key,
    setOf(ResourceItem(emptySet(), "strings.xml"))
)

@OptIn(InternalResourceApi::class, ExperimentalResourceApi::class)
internal fun TestPluralStringResource(key: String) = PluralStringResource(
    "PLURALS:$key",
    key,
    setOf(ResourceItem(emptySet(), "strings.xml"))
)

internal fun parsePluralSamples(samples: String): List<Int> {
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

internal fun pluralRuleListOf(vararg rules: Pair<PluralCategory, String>): PluralRuleList {
    val pluralRules = rules.map { PluralRule(it.first, it.second) } + PluralRule(PluralCategory.OTHER, "")
    return PluralRuleList(pluralRules.toTypedArray())
}