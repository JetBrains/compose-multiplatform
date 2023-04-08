package example.imageviewer

actual fun getCurrentLanguage(): AvailableLanguages =
    when (System.getProperties()["user.language"]) {
        "de" -> AvailableLanguages.DE
        else -> AvailableLanguages.EN
    }

actual fun getCurrentPlatform(): String = "Desktop"
