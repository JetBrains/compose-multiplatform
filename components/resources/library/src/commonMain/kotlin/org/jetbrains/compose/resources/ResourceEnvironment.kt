package org.jetbrains.compose.resources

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.intl.Locale

internal data class ResourceEnvironment(
    val language: LanguageQualifier,
    val region: RegionQualifier,
    val theme: ThemeQualifier,
    val density: DensityQualifier
)

@Composable
internal fun rememberEnvironment(): ResourceEnvironment {
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
    val withQualifier = filter { item ->
        item.qualifiers.any { it == qualifier }
    }

    if (withQualifier.isNotEmpty()) return withQualifier

    val withoutQualifier = filter { item ->
        item.qualifiers.none { it::class == qualifier::class }
    }

    if (withoutQualifier.isNotEmpty()) return withoutQualifier

    return this
}