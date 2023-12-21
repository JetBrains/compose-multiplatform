package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable

internal fun getTestEnvironment() = ResourceEnvironment(
    language = LanguageQualifier("en"),
    region = RegionQualifier("US"),
    theme = ThemeQualifier.LIGHT,
    density = DensityQualifier.XHDPI
)

internal val TestComposeEnvironment = object : ComposeEnvironment {
    @Composable
    override fun rememberEnvironment() = getTestEnvironment()
}