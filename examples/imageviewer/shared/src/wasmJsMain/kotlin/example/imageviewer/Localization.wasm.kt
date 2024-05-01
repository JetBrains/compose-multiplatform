package example.imageviewer

import kotlinx.browser.window

actual fun getCurrentLanguage(): AvailableLanguages =
    when (window.navigator.languages[0]?.toString() ?: window.navigator.language) {
        "de" -> AvailableLanguages.DE
        else -> AvailableLanguages.EN
    }

actual fun getCurrentPlatform(): String = "Web Wasm"
