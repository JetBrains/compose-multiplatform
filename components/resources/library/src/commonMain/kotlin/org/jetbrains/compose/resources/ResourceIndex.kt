package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

//TODO Here will be logic to map a static ID to a file path in resources dir
//at the moment ID = file path
internal suspend fun getPathById(id: ResourceId): String = id

@Composable
internal fun rememberFilePath(id: ResourceId): State<String> =
    rememberState(id, { "" }) { getPathById(id) }

internal val ResourceId.stringKey get() = this