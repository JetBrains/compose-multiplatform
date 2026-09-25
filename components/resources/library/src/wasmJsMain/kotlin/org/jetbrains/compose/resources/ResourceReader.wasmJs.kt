@file:OptIn(ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.resources

import org.khronos.webgl.ArrayBuffer
import org.w3c.files.Blob
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise

@JsFun("(blob) => blob.arrayBuffer()")
private external fun jsExportBlobAsArrayBuffer(blob: Blob): Promise<ArrayBuffer>

internal actual suspend fun Blob.asByteArray(): ByteArray {
    val buffer: ArrayBuffer = jsExportBlobAsArrayBuffer(this).await()
    return fastArrayBufferToByteArray(buffer)
}
