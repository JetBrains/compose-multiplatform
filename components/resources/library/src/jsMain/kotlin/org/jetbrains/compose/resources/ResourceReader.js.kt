package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array

private fun ArrayBuffer.toByteArray(): ByteArray =
    Int8Array(this, 0, byteLength).unsafeCast<ByteArray>()

@ExperimentalResourceApi
actual suspend fun readBytes(path: String): ByteArray {
    val resPath = WebResourcesConfiguration.resourcePathCustomization(path)
    val response = window.fetch(resPath).await()
    if (!response.ok) {
        throw MissingResourceException(resPath)
    }
    return response.arrayBuffer().await().toByteArray()
}

@OptIn(ExperimentalResourceApi::class)
@Composable
internal actual fun rememberBytes(id: ResourceId): State<ByteArray> {
    val state = remember(id) { mutableStateOf(ByteArray(0)) }
    LaunchedEffect(id) {
        val path = getPathById(id)
        state.value = readBytes(path)
    }
    return state
}