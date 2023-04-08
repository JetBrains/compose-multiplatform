package com.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.coroutines.CoroutineContext


/**
 * Создать репозиторий для получения tile картинок.
 * В зависимости от платформы будет обёрнут в Декоратор для кэша на диск и (или) in-memory кэш.
 * Эта функция с аннотацией Composable, чтобы можно было получить android Context
 */
@Composable
internal fun rememberTilesRepository(
    userAgent: String,
    ioScope: CoroutineScope
): ContentRepository<Tile, TileImage> = remember {
    // Для HOME директории MacOS требует разрешения.
    // Чтобы не просить разрешений созданим кэш во временной директории.
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
