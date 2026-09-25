package org.jetbrains.compose.resources

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.files.Blob
import kotlin.js.Promise

internal actual suspend fun Blob.asByteArray(): ByteArray {
    //https://developer.mozilla.org/en-US/docs/Web/API/Blob/arrayBuffer
    val buffer = asDynamic().arrayBuffer() as Promise<ArrayBuffer>
    return Int8Array(buffer.await()).unsafeCast<ByteArray>()
}
