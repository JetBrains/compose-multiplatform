package example.imageviewer

enum class AvailableLanguages {
    DE,
    EN;
}

expect fun getCurrentLanguage(): AvailableLanguages

object EnglishLocalization : Localization {
    override val appName = "My Memories"
    override val picture = "Picture:"
    override val back = "Back"
    override val takePhoto = "Take a photo 📸"
    override val addPhoto = "Add a photo"
}

object DeutschhLocalization : Localization {
    override val appName = "Meine Erinnerungen"
    override val picture = "Bild:"
    override val back = "Zurück"
    override val takePhoto = "Mach ein Foto 📸"
    override val addPhoto = "Füge ein Foto hinzu"
}
