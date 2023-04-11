package example.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.coroutines.CoroutineContext

@Composable
internal fun rememberTilesRepository(
    userAgent: String,
    ioScope: CoroutineScope
): ContentRepository<Tile, TileImage> = remember {
    val cacheDir = File(System.getProperty("java.io.tmpdir")).resolve(Config.CACHE_DIR_NAME)
    createRealRepository(HttpClient(CIO) {
        install(UserAgent) {
            agent = userAgent
        }
    })
        .decorateWithLimitRequestsInParallel(ioScope)
        .decorateWithDiskCache(ioScope, cacheDir)
        .adapter { TileImage(it.toImageBitmap()) }
        .decorateWithDistinctDownloader(ioScope)
}

internal fun getDispatcherIO(): CoroutineContext = Dispatchers.Default
