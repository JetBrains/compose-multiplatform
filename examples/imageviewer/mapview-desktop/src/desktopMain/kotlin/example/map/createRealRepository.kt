package example.map

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

fun createRealRepository(ktorClient: HttpClient) =
    object : ContentRepository<Tile, ByteArray> {
        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        override suspend fun loadContent(tile: Tile): ByteArray {
            return ktorClient.get(
                urlString = Config.createTileUrl(tile)
            ).readBytes()
        }
    }
