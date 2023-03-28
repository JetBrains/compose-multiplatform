package example.imageviewer

enum class AvailableLanguages {
    DE,
    EN;
}

expect fun getCurrentLanguage(): AvailableLanguages

object EnglishLocalization : Localization {
    override val appName = "My Memories"
    override val loading = "Loading images..."
    override val repoEmpty = "Repository is empty."
    override val noInternet = "No internet access."
    override val repoInvalid = "List of images in current repository is invalid or empty."
    override val refreshUnavailable = "Cannot refresh images."
    override val loadImageUnavailable = "Cannot load full size image."
    override val lastImage = "This is last image."
    override val firstImage = "This is first image."
    override val picture = "Picture:"
    override val size = "Size:"
    override val pixels = "pixels."
    override val back = "Back"
    override val takePhoto = "Take a photo 📸"
    override val addPhoto = "Add a photo"
}

object DeutschhLocalization : Localization {
    override val appName = "Meine Erinnerungen"
    override val loading = "Bilder werden geladen..."
    override val repoEmpty = "Bildverzeichnis ist leer."
    override val noInternet = "Kein Internetzugriff."
    override val repoInvalid = "Bildverzeichnis beschädigt oder leer."
    override val refreshUnavailable = "Kann Bilder nicht aktualisieren."
    override val loadImageUnavailable = "Kann volles Bild nicht laden."
    override val lastImage = "Dies ist das letzte Bild."
    override val firstImage = "Dies ist das erste Bild."
    override val picture = "Bild:"
    override val size = "Abmessungen:"
    override val pixels = "Pixel."
    override val back = "Zurück"
    override val takePhoto = "Mach ein Foto 📸"
    override val addPhoto = "Füge ein Foto hinzu"
}
