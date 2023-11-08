package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal suspend fun getPathById(id: ResourceId): String = id //TODO

@Composable
internal fun rememberFilePath(id: ResourceId): State<String> =
    rememberState(id, "") { getPathById(id) }

internal val ResourceId.stringKey get() = this