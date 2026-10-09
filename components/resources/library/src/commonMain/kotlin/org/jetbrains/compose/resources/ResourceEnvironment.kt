package org.jetbrains.compose.resources

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.intl.Locale

internal data class ResourceLocale(
    val language: LanguageQualifier,
    val script: ScriptQualifier,
    val region: RegionQualifier
) {
    constructor(locale: Locale) : this(
        LanguageQualifier(locale.language), ScriptQualifier(locale.script), RegionQualifier(locale.region)
    )
}

class ResourceEnvironment internal constructor(
    locales: List<ResourceLocale>,
    internal val theme: ThemeQualifier,
    internal val density: DensityQualifier
) {
    internal val locales = locales.toList().also { require(it.isNotEmpty()) }
    internal val language get() = locales.first().language
    internal val script get() = locales.first().script
    internal val region get() = locales.first().region

    internal constructor(
        language: LanguageQualifier,
        script: ScriptQualifier,
        region: RegionQualifier,
        theme: ThemeQualifier,
        density: DensityQualifier
    ) : this(listOf(ResourceLocale(language, script, region)), theme, density)

    override fun equals(other: Any?): Boolean =
        this === other || other is ResourceEnvironment &&
            locales == other.locales && theme == other.theme && density == other.density

    override fun hashCode(): Int = (locales.hashCode() * 31 + theme.hashCode()) * 31 + density.hashCode()
}

internal interface ComposeEnvironment {
    @Composable
    fun rememberEnvironment(): ResourceEnvironment
}

internal val DefaultComposeEnvironment = object : ComposeEnvironment {
    @Composable
    override fun rememberEnvironment(): ResourceEnvironment {
        val locales = rememberResourceLocales()
        val composeTheme = isSystemInDarkTheme()
        val composeDensity = LocalDensity.current

        //cache ResourceEnvironment unless compose environment is changed
        return remember(locales, composeTheme, composeDensity) {
            ResourceEnvironment(
                locales,
                ThemeQualifier.selectByValue(composeTheme),
                DensityQualifier.selectByDensity(composeDensity.density)
            )
        }
    }
}

@Composable
internal expect fun rememberResourceLocales(): List<ResourceLocale>

internal expect fun getSystemResourceLocales(): List<ResourceLocale>

//ComposeEnvironment provider will be overridden for tests
internal val LocalComposeEnvironment = staticCompositionLocalOf { DefaultComposeEnvironment }

/**
 * Returns an instance of [ResourceEnvironment].
 *
 * The [ResourceEnvironment] class represents the environment for resources.
 *
 * @return An instance of [ResourceEnvironment] representing the current environment.
 */
@Composable
fun rememberResourceEnvironment(): ResourceEnvironment {
    val composeEnvironment = LocalComposeEnvironment.current
    return composeEnvironment.rememberEnvironment()
}

internal expect fun getSystemEnvironment(): ResourceEnvironment

//the function reference will be overridden for tests
//@TestOnly
internal var getResourceEnvironment = ::getSystemEnvironment

/**
 * Provides the resource environment for non-composable access to resources.
 * It is an expensive operation! Don't use it in composable functions with no cache!
 */
fun getSystemResourceEnvironment(): ResourceEnvironment = getResourceEnvironment()

@OptIn(InternalResourceApi::class)
internal fun Resource.getResourceItemByEnvironment(environment: ResourceEnvironment): ResourceItem {
    //Priority of environments: https://developer.android.com/guide/topics/resources/providing-resources#table2
    val locale = getResourceLocale(environment)
    items.toList()
        .filterByLocale(locale.language, locale.script, locale.region)
        .also { if (it.size == 1) return it.first() }
        .filterBy(environment.theme)
        .also { if (it.size == 1) return it.first() }
        .filterByDensity(environment.density)
        .also { if (it.size == 1) return it.first() }
        .let { items ->
            if (items.isEmpty()) {
                error("Resource with ID='$id' not found")
            } else {
                error("Resource with ID='$id' has more than one file: ${items.joinToString { it.path }}")
            }
        }
}

// Resolve one language for the resource module, including entries absent from this resource.
// This keeps partial translations on the selected language and then the default resources.
internal fun Resource.getResourceLocale(environment: ResourceEnvironment): ResourceLocale {
    val supported = supportedLocales?.map { ResourceLocale(Locale(it)) }
        ?: items.mapNotNull { item ->
            val language = item.qualifiers.filterIsInstance<LanguageQualifier>().firstOrNull()
                ?: return@mapNotNull null
            ResourceLocale(
                language,
                item.qualifiers.filterIsInstance<ScriptQualifier>().firstOrNull() ?: ScriptQualifier(""),
                item.qualifiers.filterIsInstance<RegionQualifier>().firstOrNull() ?: RegionQualifier("")
            )
        }
    return environment.locales.firstOrNull { requested ->
        // Android treats unqualified resources as supporting English when selecting the app locale.
        requested.language.language == "en" || supported.any { available ->
            requested.language == available.language &&
                (requested.script.isEmpty() || available.script.isEmpty() || requested.script == available.script)
        }
    } ?: environment.locales.first()
}

private fun List<ResourceItem>.filterBy(qualifier: Qualifier): List<ResourceItem> {
    //Android has a slightly different algorithm,
    //but it provides the same result: https://developer.android.com/guide/topics/resources/providing-resources#BestMatch

    //filter items with the requested qualifier
    val withQualifier = filter { item ->
        item.qualifiers.any { it == qualifier }
    }

    if (withQualifier.isNotEmpty()) return withQualifier

    //items with no requested qualifier type (default)
    return filter { item ->
        item.qualifiers.none { it::class == qualifier::class }
    }
}

// https://developer.android.com/guide/topics/resources/providing-resources#BestMatch
// In general, Android prefers scaling down a larger original image to scaling up a smaller original image.
private fun List<ResourceItem>.filterByDensity(density: DensityQualifier): List<ResourceItem> {
    val items = this
    var withQualifier = emptyList<ResourceItem>()

    // filter with the same or better density
    val exactAndHigherQualifiers = DensityQualifier.entries
        .filter { it.dpi >= density.dpi }
        .sortedBy { it.dpi }

    for (qualifier in exactAndHigherQualifiers) {
        withQualifier = items.filter { item -> item.qualifiers.any { it == qualifier } }
        if (withQualifier.isNotEmpty()) break
    }
    if (withQualifier.isNotEmpty()) return withQualifier

    // filter with low density
    val lowQualifiers = DensityQualifier.entries
        .minus(DensityQualifier.LDPI)
        .filter { it.dpi < density.dpi }
        .sortedByDescending { it.dpi }
    for (qualifier in lowQualifiers) {
        withQualifier = items.filter { item -> item.qualifiers.any { it == qualifier } }
        if (withQualifier.isNotEmpty()) break
    }
    if (withQualifier.isNotEmpty()) return withQualifier

    //items with no DensityQualifier (default)
    // The system assumes that default resources (those from a directory without configuration qualifiers)
    // are designed for the baseline pixel density (mdpi) and resizes those bitmaps
    // to the appropriate size for the current pixel density.
    // https://developer.android.com/training/multiscreen/screendensities#DensityConsiderations
    val withNoDensity = items.filter { item ->
        item.qualifiers.none { it is DensityQualifier }
    }
    if (withNoDensity.isNotEmpty()) return withNoDensity

    //items with LDPI density
    return items.filter { item ->
        item.qualifiers.any { it == DensityQualifier.LDPI }
    }
}

// Filter by language, script, and region together (extended from the original lang+region logic):
// 1) exact language + script + region -> use it
// 2) language + script (no region) -> use it
// 3) language + region (no script) -> use it
// 4) language only (no script, no region) -> use it
// 5) items with NO locale qualifiers at all (default)
// When the environment script is empty (e.g. DefaultComposeEnvironment), prefer items without
// a ScriptQualifier first; fall back to script-tagged items only if nothing else matches.
// issue: https://github.com/JetBrains/compose-multiplatform/issues/4571
private fun List<ResourceItem>.filterByLocale(
    language: LanguageQualifier,
    script: ScriptQualifier,
    region: RegionQualifier
): List<ResourceItem> {
    // Case 5: items with NO locale qualifiers at all (default)
    val noLocaleItems = filter { item ->
        item.qualifiers.none { it is LanguageQualifier || it is ScriptQualifier || it is RegionQualifier }
    }

    val withLanguage = filter { item ->
        item.qualifiers.any { it == language }
    }
    if (withLanguage.isEmpty()) return noLocaleItems

    // Case 1 & 2: language + script items, narrowed by region (exact script+region or script only)
    val withScript = withLanguage.filter { item ->
        item.qualifiers.any { it == script }
    }
    val byScriptAndRegion = withScript.filterBy(region)
    if (byScriptAndRegion.isNotEmpty()) return byScriptAndRegion

    // Case 3 & 4: language items without a script qualifier, narrowed by region (exact region or language only)
    val withDefaultScript = withLanguage.filter { item ->
        item.qualifiers.none { it is ScriptQualifier }
    }
    val byDefaultScriptAndRegion = withDefaultScript.filterBy(region)
    if (byDefaultScriptAndRegion.isNotEmpty()) return byDefaultScriptAndRegion

    // Fallback: don't cross scripts when one was requested (zh-Hans must not fall back to zh-Hant)
    // When the environment script is empty, fall back to script-tagged items only if nothing else matches.
    if (script.isEmpty()) {
        val byRegion = withLanguage.filterBy(region)
        if (byRegion.isNotEmpty()) return byRegion
    }

    return noLocaleItems
}