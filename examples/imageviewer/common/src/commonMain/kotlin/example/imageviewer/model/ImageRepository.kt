package example.imageviewer.model

import example.imageviewer.core.Repository
import example.imageviewer.utils.ktorHttpClient
import example.imageviewer.utils.runBlocking
import io.ktor.client.request.*

class ImageRepository(
    private val httpsURL: String
) : Repository<MutableList<String>> {

    override fun get(): MutableList<String> {
        return runBlocking {
            val content = ktorHttpClient.get<String>(httpsURL)
            content.lines().toMutableList()
        }
    }
}
