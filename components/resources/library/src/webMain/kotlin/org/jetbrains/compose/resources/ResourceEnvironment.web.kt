@file:OptIn(ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.browser.window
import org.w3c.dom.events.Event
import kotlin.collections.ifEmpty
import kotlin.collections.map

private external class Intl {
    class Locale(locale: JsString) {
        val language: String

        // Intl.Locale.script can be undefined.
        // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/Locale/script
        val script: String?

        // Intl.Locale.region can be undefined.
        // For example, new Int.Locale('en') instead of new Int.Locale('en-NL').
        // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/Locale/region
        val region: String?
    }
}

@Composable
internal actual fun getSystemLocales(): List<ResourceLocale> {
    var locales by remember { mutableStateOf(getSystemResourceLocales()) }
    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = { locales = getSystemResourceLocales() }
        window.addEventListener("languagechange", listener)
        onDispose { window.removeEventListener("languagechange", listener) }
    }
    return locales
}

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    val locales = getSystemResourceLocales()
    val isDarkTheme = window.matchMedia("(prefers-color-scheme: dark)").matches
    //96 - standard browser DPI https://developer.mozilla.org/en-US/docs/Web/API/Window/devicePixelRatio
    val dpi: Int = (window.devicePixelRatio * 96).toInt()
    return ResourceEnvironment(
        locales = locales,
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByValue(dpi)
    )
}

private fun getSystemResourceLocales(): List<ResourceLocale> =
    window.navigator.languages.toList().ifEmpty { listOf(window.navigator.language.toJsString()) }.map { tag ->
        val locale = Intl.Locale(tag)
        ResourceLocale(
            LanguageQualifier(locale.language),
            ScriptQualifier(locale.script.orEmpty()),
            RegionQualifier(locale.region.orEmpty())
        )
    }