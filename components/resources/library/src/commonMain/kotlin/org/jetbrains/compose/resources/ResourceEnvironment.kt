package org.jetbrains.compose.resources

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList

class ResourceEnvironment internal constructor(
    internal val locales: List<LocaleQualifiers>,
    internal val theme: ThemeQualifier,
    internal val density: DensityQualifier
) {
    internal val language: LanguageQualifier get() = locales.firstOrNull()?.language ?: LanguageQualifier("")
    internal val region: RegionQualifier get() = locales.firstOrNull()?.region ?: RegionQualifier("")

    internal constructor(
        language: LanguageQualifier,
        script: ScriptQualifier,
        region: RegionQualifier,
        theme: ThemeQualifier,
        density: DensityQualifier
    ) : this(
        locales = listOf(LocaleQualifiers(language, script, region)),
        theme = theme,
        density = density
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ResourceEnvironment

        if (locales != other.locales) return false
        if (theme != other.theme) return false
        if (density != other.density) return false

        return true
    }

    override fun hashCode(): Int {
        var result = locales.hashCode()
        result = 31 * result + theme.hashCode()
        result = 31 * result + density.hashCode()
        return result
    }
}

internal interface ComposeEnvironment {
    @Composable
    fun rememberEnvironment(): ResourceEnvironment
}

internal val DefaultComposeEnvironment = object : ComposeEnvironment {
    @Composable
    override fun rememberEnvironment(): ResourceEnvironment {
        val composeLocales = LocaleList.current
        val composeTheme = isSystemInDarkTheme()
        val composeDensity = LocalDensity.current

        //cache ResourceEnvironment unless compose environment is changed
        return remember(composeLocales, composeTheme, composeDensity) {
            val composeMapped = if (composeLocales.isEmpty()) {
                val single = Locale.current
                listOf(
                    LocaleQualifiers(
                        LanguageQualifier(single.language),
                        ScriptQualifier(single.script),
                        RegionQualifier(single.region)
                    )
                )
            } else {
                composeLocales.map {
                    LocaleQualifiers(
                        LanguageQualifier(it.language),
                        ScriptQualifier(it.script),
                        RegionQualifier(it.region)
                    )
                }
            }
            val locales = selectResourceLocales(composeMapped, getPreferredLocales())
            ResourceEnvironment(
                locales = locales,
                theme = ThemeQualifier.selectByValue(composeTheme),
                density = DensityQualifier.selectByDensity(composeDensity.density)
            )
        }
    }
}

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

/**
 * Chooses the locale list used for resource matching.
 *
 * [androidx.compose.ui.text.intl.LocaleList] is used as-is when it already contains a
 * fallback chain, which is the case on Android API 24+ and on the web.
 *
 * On JVM desktop it holds a single platform-default locale even when the OS has a preference
 * list, so the OS list supplies the missing fallback chain. The Compose locale stays in front
 * of it: an app that calls `Locale.setDefault` to pick its language is indistinguishable here
 * from a platform default, and dropping it would ignore the app.
 */
internal fun selectResourceLocales(
    composeLocales: List<LocaleQualifiers>,
    systemLocales: List<LocaleQualifiers>
): List<LocaleQualifiers> {
    if (composeLocales.isEmpty()) return systemLocales
    if (composeLocales.size > 1 || systemLocales.size <= 1) return composeLocales
    val compose = composeLocales.single()
    return listOf(compose) + systemLocales.filterNot { it == compose }
}

/**
 * The locales the user prefers, most preferred first.
 *
 * Kept separate from [getSystemEnvironment] so that composition only pays for the locales:
 * resolving the theme and the density is more expensive, and [DefaultComposeEnvironment]
 * takes both from Compose instead.
 */
internal expect fun systemPreferredLocales(): List<LocaleQualifiers>

internal expect fun getSystemEnvironment(): ResourceEnvironment

//the function references will be overridden for tests
//@TestOnly
internal var getResourceEnvironment = ::getSystemEnvironment

//@TestOnly
internal var getPreferredLocales = ::systemPreferredLocales

/**
 * Provides the resource environment for non-composable access to resources.
 * It is an expensive operation! Don't use it in composable functions with no cache!
 */
fun getSystemResourceEnvironment(): ResourceEnvironment = getResourceEnvironment()

@OptIn(InternalResourceApi::class)
internal fun Resource.getResourceItemByEnvironment(environment: ResourceEnvironment): ResourceItem {
    //Priority of environments: https://developer.android.com/guide/topics/resources/providing-resources#table2
    items.toList()
        .filterByLocales(environment.locales)
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

// Filter by prioritized list of locales.
// For each locale in the prioritized list (e.g. [fr-CA, es-US, en-US]):
// 1) exact language + script + region -> use it
// 2) language + script (no region) -> use it
// 3) language + region (no script) -> use it
// 4) language only (no script, no region) -> use it
// 5) same language, another region of the same script (Android parent/child locale, fr-CH -> fr-FR)
// Step 5 is skipped when the locale names a script, and otherwise compares the script the
// region implies, so neither zh-Hans nor zh-CN ever lands on values-zh-rTW.
// If none of the preferred locales match:
// 6) items with NO locale qualifiers at all (default)
// When the environment script is empty, prefer items without
// a ScriptQualifier first; fall back to script-tagged items only if nothing else matches.
// https://youtrack.jetbrains.com/issue/CMP-6840
// https://github.com/JetBrains/compose-multiplatform/issues/4571
private fun List<ResourceItem>.filterByLocales(
    locales: List<LocaleQualifiers>
): List<ResourceItem> {
    for (locale in locales) {
        val matched = filterBySingleLocale(locale.language, locale.script, locale.region)
        if (matched.isNotEmpty()) {
            return matched
        }
    }

    // Default: items with NO locale qualifiers at all
    return filter { item ->
        item.qualifiers.none { it is LanguageQualifier || it is ScriptQualifier || it is RegionQualifier }
    }
}

private fun ResourceItem.scriptQualifier(): ScriptQualifier? =
    qualifiers.filterIsInstance<ScriptQualifier>().firstOrNull()

private fun ResourceItem.regionQualifier(): RegionQualifier? =
    qualifiers.filterIsInstance<RegionQualifier>().firstOrNull()

private val TRADITIONAL_CHINESE_REGIONS = setOf("TW", "HK", "MO")

// Android computes a script for a locale that does not name one and never matches across
// scripts, so values-zh-rCN and values-zh-rTW do not substitute for each other. See
// ResTable_config::computeScript and localeDataComputeScript. Chinese is the language where
// the region implies the script; null means the region constrains nothing.
private fun likelyScript(language: String, region: String): String? = when (language) {
    "zh" -> if (region in TRADITIONAL_CHINESE_REGIONS) "Hant" else "Hans"
    else -> null
}

private fun List<ResourceItem>.narrowByRegion(
    language: LanguageQualifier,
    region: RegionQualifier,
    allowOtherRegions: Boolean
): List<ResourceItem> {
    if (isEmpty()) return emptyList()
    val regionCode = region.region
    if (regionCode.isNotEmpty()) {
        val exact = filter { it.regionQualifier()?.region == regionCode }
        if (exact.isNotEmpty()) return exact
    }
    val noRegion = filter { it.regionQualifier() == null }
    if (noRegion.isNotEmpty()) return noRegion
    if (!allowOtherRegions) return emptyList()
    // Android falls back to another region of the same language: fr-CH uses fr-FR. It ranks the
    // candidates with the CLDR region-distance table, which is not bundled here, so the lowest
    // region code wins and the result is the same on every run. Every item of that region is
    // kept so that theme and density filtering still runs.
    val wantedScript = likelyScript(language.language, regionCode)
    val candidates = filter { likelyScript(language.language, it.regionQualifier()?.region.orEmpty()) == wantedScript }
    val fallbackRegion = candidates.mapNotNull { it.regionQualifier()?.region }.minOrNull() ?: return emptyList()
    return filter { it.regionQualifier()?.region == fallbackRegion }
}

private fun ResourceItem.effectiveScript(language: String): String? =
    scriptQualifier()?.script?.takeIf { it.isNotEmpty() }
        ?: likelyScript(language, regionQualifier()?.region.orEmpty())

private fun List<ResourceItem>.filterBySingleLocale(
    language: LanguageQualifier,
    script: ScriptQualifier,
    region: RegionQualifier
): List<ResourceItem> {
    val withLanguage = filter { item ->
        item.qualifiers.any { it == language }
    }
    if (withLanguage.isEmpty()) return emptyList()

    val wantedScript = script.script.ifEmpty { likelyScript(language.language, region.region).orEmpty() }
    val scriptCompatible = if (wantedScript.isNotEmpty()) {
        withLanguage.filter {
            val itemScript = it.effectiveScript(language.language)
            itemScript == null || itemScript == wantedScript
        }
    } else {
        withLanguage
    }
    if (scriptCompatible.isEmpty()) return emptyList()

    if (!script.isEmpty()) {
        val explicitScript = scriptCompatible.filter { it.scriptQualifier() == script }
        val explicitMatch = explicitScript.narrowByRegion(language, region, allowOtherRegions = false)
        if (explicitMatch.isNotEmpty()) return explicitMatch

        val withoutScript = scriptCompatible.filter { it.scriptQualifier() == null }
        return withoutScript.narrowByRegion(language, region, allowOtherRegions = false)
    }

    if (wantedScript.isNotEmpty()) {
        val explicitScript = scriptCompatible.filter { it.scriptQualifier()?.script == wantedScript }
        val explicitMatch = explicitScript.narrowByRegion(language, region, allowOtherRegions = true)
        if (explicitMatch.isNotEmpty()) return explicitMatch

        val withoutScript = scriptCompatible.filter { it.scriptQualifier() == null }
        return withoutScript.narrowByRegion(language, region, allowOtherRegions = true)
    }

    val withDefaultScript = withLanguage.filter { it.scriptQualifier() == null }
    val defaultScriptMatch = withDefaultScript.narrowByRegion(language, region, allowOtherRegions = true)
    if (defaultScriptMatch.isNotEmpty()) return defaultScriptMatch

    return withLanguage.narrowByRegion(language, region, allowOtherRegions = true)
}
