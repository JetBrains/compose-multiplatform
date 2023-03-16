package example.imageviewer.model

interface ContentRepository<T> {
    suspend fun loadContent(picture: Picture): T
}
