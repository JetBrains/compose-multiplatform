package example.imageviewer.model

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

interface ContentRepository<T> {
    suspend fun loadContent(url: String): T
}

fun createNetworkRepository(ktorClient: HttpClient) = object : ContentRepository<ByteArray> {
    override suspend fun loadContent(url: String): ByteArray =
        ktorClient.get(urlString = url).readBytes()
}

fun <A, B> ContentRepository<A>.adapter(transform: (A) -> B): ContentRepository<B> {
    val origin = this
    return object : ContentRepository<B> {
        override suspend fun loadContent(url: String): B {
            return transform(origin.loadContent(url))
        }
    }
}
