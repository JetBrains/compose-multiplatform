package example.imageviewer.model

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

interface ContentRepository<T> {
    suspend fun loadContent(url: String): T
}

fun createRealRepository(ktorClient: HttpClient) =
    object : ContentRepository<ByteArray> {
        override suspend fun loadContent(url: String): ByteArray =
            ktorClient.get(urlString = url).readBytes()
    }
