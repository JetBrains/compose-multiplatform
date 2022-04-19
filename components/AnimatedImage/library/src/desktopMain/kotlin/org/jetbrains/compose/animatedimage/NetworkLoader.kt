package org.jetbrains.compose.animatedimage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

internal class NetworkLoader(private val imageUrl: String) : AnimatedImageLoader {
    var cachedBytes: ByteArray? = null

    override suspend fun loadBytes(): ByteArray = withContext(Dispatchers.IO) {
        var bytesArray: ByteArray? = cachedBytes

        if (bytesArray == null) {
            bytesArray = loadNetworkResource(imageUrl)

            cachedBytes = bytesArray
        }

        return@withContext bytesArray
    }
}

private suspend fun loadNetworkResource(imageUrl: String): ByteArray = withContext(Dispatchers.IO) {
    val url = URL(imageUrl)
    val connection = url.openConnection() as HttpURLConnection
    connection.connect()

    connection.inputStream.readAllBytes()
}