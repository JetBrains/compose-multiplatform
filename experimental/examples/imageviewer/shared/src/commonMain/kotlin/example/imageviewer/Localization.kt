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
    override val kotlinConfName = "Kotlin Conf 2023 🎉"
    override val kotlinConfDescription = """
        This photo was taken during Kotlin Conf 2023! 🎊
        Have a fun with Kotlin and Compose Multiplatform 🥳
    """.trimIndent()
    override val newPhotoName = "New Memory"
    override val newPhotoDescription = "May amazing things happen to you! 🙂"
}

object DeutschLocalization : Localization {
    override val appName = "Meine Erinnerungen"
    override val picture = "Bild:"
    override val back = "Zurück"
    override val takePhoto = "Mach ein Foto 📸"
    override val addPhoto = "Füge ein Foto hinzu"
    override val kotlinConfName = "Kotlin Conf 2023 🎉"
    override val kotlinConfDescription = """
        This photo was taken during Kotlin Conf 2023! 🎊
        Have a fun with Kotlin and Compose Multiplatform 🥳
    """.trimIndent()
    override val newPhotoName = "New Memory"
    override val newPhotoDescription = "May amazing things happen to you! 🙂"
}
