package example.imageviewer.model

expect suspend fun loadFullImage(source: String): Picture

expect suspend fun loadImages(cachePath: String, list: List<String>): List<Picture>
