package org.jetbrains.compose.resources

import androidx.compose.runtime.*
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.intl.Locale

internal data class ResourceEnvironment(
    val language: LanguageQualifier,
    val region: RegionQualifier,
    val theme: ThemeQualifier,
    val density: DensityQualifier
)

@OptIn(InternalComposeApi::class)
@Composable
internal fun rememberEnvironment(): ResourceEnvironment {
    val composeLocale = Locale.current
    val composeTheme = LocalSystemTheme.current
    val composeDensity = LocalDensity.current

    //cache ResourceEnvironment unless compose environment is changed
    //TODO provide top level function with a single cache in a root of compose tree
    return remember(composeLocale, composeTheme, composeDensity) {
        ResourceEnvironment(
            LanguageQualifier(composeLocale.language),
            RegionQualifier(composeLocale.region),
            ThemeQualifier.selectByValue(composeTheme == SystemTheme.Dark),
            DensityQualifier.selectByDensity(composeDensity.density)
        )
    }
}

/**
 * Provides the resource environment for non-composable access to string resources.
 * It is an expensive operation! Don't use it in composable functions with no cache!
 */
internal expect fun getResourceEnvironment(): ResourceEnvironment

internal fun Resource.getPathByEnvironment(environment: ResourceEnvironment): String {
    items.toList()
        .filterBy(environment.language)
        .also { if (it.size == 1) return it.first().path }
        .filterBy(environment.region)
        .also { if (it.size == 1) return it.first().path }
        .filterBy(environment.theme)
        .also { if (it.size == 1) return it.first().path }
        .filterBy(environment.density)
        .also { if (it.size == 1) return it.first().path }
        .let { return it.first().path }
}

private fun List<ResourceItem>.filterBy(qualifier: Qualifier): List<ResourceItem> {
    val items = map { it to it.qualifiers.toList().parseQualifiers() }
    val withQualifier = items.filter { (_, qualifiers) ->
        qualifiers.any { it == qualifier }
    }.map { (item, _) -> item }

    if (withQualifier.isNotEmpty()) return withQualifier

    val withoutQualifier = items.filter { (_, qualifiers) ->
        qualifiers.none { it::class == qualifier::class }
    }.map { (item, _) -> item }

    if (withoutQualifier.isNotEmpty()) return withoutQualifier

    return this
}