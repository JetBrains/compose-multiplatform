package example.imageviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import example.imageviewer.model.ContentRepository
import example.imageviewer.model.NetworkRequest
import example.imageviewer.model.createRealRepository
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberImageRepository(
    ioScope: CoroutineScope
): ContentRepository<NetworkRequest, ImageBitmap> {
    val cacheDir = LocalContext.current.cacheDir
    return remember(cacheDir) {
        createRealRepository(HttpClient(CIO))
            .decorateWithDiskCache(ioScope, cacheDir)
            .adapter { it.toImageBitmap() }
    }
}
