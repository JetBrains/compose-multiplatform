package org.jetbrains.compose.resources

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.intl.Locale

class ResourceEnvironment internal constructor(
    internal val language: LanguageQualifier,
    internal val region: RegionQualifier,
    internal val theme: ThemeQualifier,
    internal val density: DensityQualifier
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ResourceEnvironment

        if (language != other.language) return false
        if (region != other.region) return false
        if (theme != other.theme) return false
        if (density != other.density) return false

        return true
    }

    override fun hashCode(): Int {
        var result = language.hashCode()
        result = 31 * result + region.hashCode()
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
        val composeLocale = Locale.current
        val composeTheme = isSystemInDarkTheme()
        val composeDensity = LocalDensity.current

        //cache ResourceEnvironment unless compose environment is changed
        return remember(composeLocale, composeTheme, composeDensity) {
            ResourceEnvironment(
                LanguageQualifier(composeLocale.language),
                RegionQualifier(composeLocale.region),
                ThemeQualifier.selectByValue(composeTheme),
                DensityQualifier.selectByDensity(composeDensity.density)
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
    items.toList()
        .filterByLocale(environment.language, environment.region)
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

// we need to filter by language and region together because there is slightly different logic:
// 1) if there is the exact match language+region then use it
// 2) if there is the language WITHOUT region match then use it
// 3) in other cases use items WITHOUT language and region qualifiers at all
// issue: https://github.com/JetBrains/compose-multiplatform/issues/4571
private fun List<ResourceItem>.filterByLocale(
    language: LanguageQualifier,
    region: RegionQualifier
): List<ResourceItem> {
    val withLanguage = filter { item ->
        item.qualifiers.any { it == language }
    }

    val withExactLocale = withLanguage.filter { item ->
        item.qualifiers.any { it == region }
    }

    //if there are the exact language + the region items
    if (withExactLocale.isNotEmpty()) return withExactLocale

    val withDefaultRegion = withLanguage.filter { item ->
        item.qualifiers.none { it is RegionQualifier }
    }

    //if there are the language without a region items
    if (withDefaultRegion.isNotEmpty()) return withDefaultRegion

    //items without any locale qualifiers
    return filter { item ->
        item.qualifiers.none { it is LanguageQualifier || it is RegionQualifier }
    }
}