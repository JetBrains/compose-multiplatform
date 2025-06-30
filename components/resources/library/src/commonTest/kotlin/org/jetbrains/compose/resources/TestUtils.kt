package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import org.jetbrains.compose.resources.plural.PluralCategory
import org.jetbrains.compose.resources.plural.PluralRule
import org.jetbrains.compose.resources.plural.PluralRuleList

private val cvrMap: Map<String, ResourceItem> = mapOf(
    "accentuated_characters" to ResourceItem(setOf(), "strings.cvr", 259, 54),
    "app_name" to ResourceItem(setOf(), "strings.cvr", 314, 44),
    "hello" to ResourceItem(setOf(), "strings.cvr", 359, 37),
    "str_template" to ResourceItem(setOf(), "strings.cvr", 397, 76),

    "another_plurals" to ResourceItem(setOf(), "strings.cvr", 10, 71),
    "messages" to ResourceItem(setOf(), "strings.cvr", 82, 88),
    "plurals" to ResourceItem(setOf(), "strings.cvr", 171, 39),

    "str_arr" to ResourceItem(setOf(), "strings.cvr", 211, 47),
)

internal fun TestStringResource(key: String) = StringResource(
    "STRING:$key",
    key,
    setOf(cvrMap[key] ?: error("String ID=`$key` is not found!"))
)

internal fun TestStringArrayResource(key: String) = StringArrayResource(
    "STRING:$key",
    key,
    setOf(cvrMap[key] ?: error("String ID=`$key` is not found!"))
)

internal fun TestPluralStringResource(key: String) = PluralStringResource(
    "PLURALS:$key",
    key,
    setOf(cvrMap[key] ?: error("String ID=`$key` is not found!"))
)

internal fun TestDrawableResource(path: String) = DrawableResource(
    path,
    setOf(ResourceItem(emptySet(), path, -1, -1))
)

internal fun TestFontResource(path: String) = FontResource(
    path,
    setOf(ResourceItem(emptySet(), path, -1, -1))
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

/**
 * Executes a test block within a Compose UI testing environment while ensuring
 * that any cached resources are cleared before the test begins.
 */
@OptIn(ExperimentalTestApi::class)
fun runComposeResourceTest(block: ComposeUiTest.() -> Unit) = runComposeUiTest {
    var areCachesCleared = false
    setContent {
        LaunchedEffect(Unit) {
            ResourceCaches.asyncClear()
            areCachesCleared = true
        }
    }
    waitUntil { areCachesCleared }
    block()
}

/**
 * Injects resource cache clearing to the content and returns a callback to clear resource caches.
 * Used for tests that need to test some conditions after cache clearing.
 * Note: a returned call-back can be used only once.
 */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.setContentWithResourceCacheCleaning(content: @Composable () -> Unit): ComposeUiTest.()->Unit {
    var clearCaches by mutableStateOf(false)
    var areCachesCleared = false

    this.setContent {
        content()
        LaunchedEffect(clearCaches) {
            if (clearCaches) {
                ResourceCaches.asyncClear()
                areCachesCleared = true
            }
        }
    }

    return {
        clearCaches = true
        waitUntil { areCachesCleared }
    }
}


