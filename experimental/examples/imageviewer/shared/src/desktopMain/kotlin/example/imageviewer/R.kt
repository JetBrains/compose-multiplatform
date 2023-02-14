package example.imageviewer

object ResString {

    val appName: String
    val loading: String
    val repoEmpty: String
    val noInternet: String
    val repoInvalid: String
    val refreshUnavailable: String
    val loadImageUnavailable: String
    val lastImage: String
    val firstImage: String
    val picture: String
    val size: String
    val pixels: String
    val back: String
    val refresh: String

    init {
        println(System.getProperty("user.language"))
        if (System.getProperty("user.language").equals("de")) {
            appName = "ImageViewer"
            loading = "Bilder werden geladen..."
            repoEmpty = "Bildverzeichnis ist leer."
            noInternet = "Kein Internetzugriff."
            repoInvalid = "Bildverzeichnisch beschädigt oder leer."
            refreshUnavailable = "Kann Bilder nicht aktualisieren."
            loadImageUnavailable = "Kann volles Bild nicht laden."
            lastImage = "Dies ist das letzte Bild."
            firstImage = "Dies ist das erste Bild."
            picture = "Bild:"
            size = "Abmessungen:"
            pixels = "Pixel."
            back = "Zurück"
            refresh = "Aktualisieren"
        } else {
            appName = "ImageViewer"
            loading = "Loading images..."
            repoEmpty = "Repository is empty."
            noInternet = "No internet access."
            repoInvalid = "List of images in current repository is invalid or empty."
            refreshUnavailable = "Cannot refresh images."
            loadImageUnavailable = "Cannot load full size image."
            lastImage = "This is last image."
            firstImage = "This is first image."
            picture = "Picture:"
            size = "Size:"
            pixels = "pixels."
            back = "Back"
            refresh = "Refresh"
        }
    }
}
