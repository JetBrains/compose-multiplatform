package example.imageviewer.model

import io.ktor.client.*
import io.ktor.client.request.*

expect suspend fun loadImages(cachePath: String, list: List<String>): List<Picture>

public interface ContentRepository<K, T> {
    public abstract suspend fun loadContent(key: K): T
}

fun createRealRepository(ktorClient: HttpClient) =
    object : ContentRepository<NetworkRequest, ByteArray> {
        override suspend fun loadContent(key: NetworkRequest): ByteArray {
            val result = ktorClient.get<ByteArray>(
                urlString = key.url
            )
            return result
        }
    }

