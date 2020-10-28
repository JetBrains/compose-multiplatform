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

    init {
        if (System.getProperty("user.language").equals("ru")) {
            appName = "ImageViewer"
            loading = "Загружаем изображения..."
            repoEmpty = "Репозиторий пуст."
            noInternet = "Нет доступа в интернет."
            repoInvalid = "Список изображений в репозитории пуст или имеет неверный формат."
            refreshUnavailable = "Невозможно обновить изображения."
            loadImageUnavailable = "Невозможно загузить полное изображение."
            lastImage = "Это последнее изображение."
            firstImage = "Это первое изображение."
            picture = "Изображение:"
            size = "Размеры:"
            pixels = "пикселей."
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
        }
    }
}
