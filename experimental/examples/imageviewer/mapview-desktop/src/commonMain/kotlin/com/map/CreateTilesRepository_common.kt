package com.map

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/**
 * Создать репозиторий для получения tile картинок.
 * В зависимости от платформы будет обёрнут в Декоратор для кэша на диск и (или) in-memory кэш.
 * Эта функция с аннотацией Composable, чтобы можно было получить android Context
 */
@Composable
internal expect fun rememberTilesRepository(
    ioScope: CoroutineScope
): ContentRepository<Tile, TileImage>

internal expect fun getDispatcherIO(): CoroutineContext
