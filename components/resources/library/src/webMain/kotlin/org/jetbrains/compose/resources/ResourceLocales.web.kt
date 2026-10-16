package org.jetbrains.compose.resources

import androidx.compose.runtime.*
import kotlinx.browser.window
import org.w3c.dom.events.Event

@Composable
internal actual fun rememberResourceLocales(): List<ResourceLocale> {
    var locales by remember { mutableStateOf(getSystemResourceLocales()) }
    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = { locales = getSystemResourceLocales() }
        window.addEventListener("languagechange", listener)
        onDispose { window.removeEventListener("languagechange", listener) }
    }
    return locales
}
