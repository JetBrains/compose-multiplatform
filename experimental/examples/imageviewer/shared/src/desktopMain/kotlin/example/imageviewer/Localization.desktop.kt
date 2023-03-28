package example.imageviewer

actual fun getCurrentLanguage(): AvailableLanguages =
    if (System.getProperty("user.language").equals("de")) {
        AvailableLanguages.DE
    } else {
        AvailableLanguages.EN
    }
