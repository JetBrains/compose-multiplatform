package example.imageviewer.model

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

public interface ContentRepository<K, T> {
    public abstract suspend fun loadContent(key: K): T
}

fun createRealRepository(ktorClient: HttpClient) =
    object : ContentRepository<NetworkRequest, ByteArray> {
        override suspend fun loadContent(key: NetworkRequest): ByteArray =
            ktorClient.get(urlString = key.url).readBytes()
    }
