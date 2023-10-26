package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.runBlocking

@ExperimentalResourceApi
@Composable
internal actual fun rememberBytes(id: ResourceId): State<ByteArray> =
    remember(id) {
        val arr = runBlocking {
            val path = getPathById(id)
            readBytes(path)
        }
        mutableStateOf(arr)
    }