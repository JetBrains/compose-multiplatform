package com.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.coroutines.CoroutineContext

@Composable
internal actual fun rememberTilesRepository(
    ioScope: CoroutineScope
): ContentRepository<Tile, TileImage> = remember {
    // Для HOME директории MacOS требует разрешения.
    // Чтобы не просить разрешений созданим кэш во временной директории.
    val cacheDir = File(System.getProperty("java.io.tmpdir")).resolve(Config.CACHE_DIR_NAME)
    createRealRepository(HttpClient(CIO))
        .decorateWithLimitRequestsInParallel(ioScope)
        .decorateWithDiskCache(ioScope, cacheDir)
        .adapter { TileImage(it.toImageBitmap()) }
        .decorateWithDistinctDownloader(ioScope)
}

internal actual fun getDispatcherIO(): CoroutineContext = Dispatchers.Default
