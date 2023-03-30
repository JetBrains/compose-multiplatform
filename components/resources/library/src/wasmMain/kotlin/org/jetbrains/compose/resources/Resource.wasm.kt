/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import androidx.compose.ui.graphics.ImageBitmap
import org.jetbrains.compose.resources.vector.xmldom.Element
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.Uint8ClampedArray
import org.w3c.xhr.ARRAYBUFFER
import org.khronos.webgl.get
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.wasm.unsafe.*
import org.w3c.files.Blob
import kotlin.wasm.unsafe.withScopedMemoryAllocator
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi

@ExperimentalResourceApi
actual fun resource(path: String): Resource = JSResourceImpl(path)

actual typealias ResourcesRawResult = Blob

external interface WebImageBitmap {
    val height: Int
    val width: Int
}

external interface WebImageData {
    val data: Uint8ClampedArray
    val colorSpace: String
    val height: Int
    val width: Int
}

actual typealias ResourcesRawImageResult = WebImageBitmap

actual suspend fun ResourcesRawResult.asResourcesRawImageResult(): ResourcesRawImageResult {
    return suspendCoroutine { continuation ->
        getWebImageBitmap(this) { webImageBitmap ->
            continuation.resume(webImageBitmap)
        }
    }
}

@Suppress(
    "INVISIBLE_MEMBER",
    "INVISIBLE_REFERENCE",
    "EXPOSED_PARAMETER_TYPE"
)
internal actual fun ResourcesRawImageResult.rawToImageBitmap(): ImageBitmap {
    val webImageData = getWebImageData(this)
    val int8Array = Int8Array(webImageData.data.buffer)
//    val int8Array = Int8Array(webImageData.data.length)
    //int8Array.set(webImageData.data)
    val pixelsByteArray = jsInt8ArrayToKotlinByteArray(int8Array)
    val bitmap = Bitmap()
    bitmap.setImageInfo(ImageInfo.makeN32(webImageData.width, webImageData.height, ColorAlphaType.OPAQUE))
    bitmap.allocPixels()
    bitmap.installPixels(pixelsByteArray)
    return androidx.compose.ui.graphics.SkiaBackedImageBitmap(bitmap)
}


@JsFun("""
    (blob, resultCallback) => {
       createImageBitmap(blob).then((res) => resultCallback(res));
    }
""")
internal external fun getWebImageBitmap(blob: Blob, resultCallback: (WebImageBitmap) -> Unit)

@JsFun("""
    (bitmap) => {
        const offscreenCanvas = new OffscreenCanvas(bitmap.width, bitmap.height);
        const context = offscreenCanvas.getContext('2d');
        context.drawImage(bitmap, 0, 0);
        return context.getImageData(0, 0, bitmap.width, bitmap.height);
    }
""")
internal external fun getWebImageData(bitmap: WebImageBitmap): WebImageData

@ExperimentalResourceApi
private class JSResourceImpl(path: String) : AbstractResourceImpl(path) {
    override suspend fun readBytes(): ResourcesRawResult {
        return suspendCoroutine { continuation ->
            val req = XMLHttpRequest()
            req.open("GET", "$path", true)
            req.responseType = "blob".asDynamic().unsafeCast<XMLHttpRequestResponseType>()

            req.onload = { _ ->
                val blob = req.response
                if (blob is Blob) {
                    //val size = arrayBuffer.byteLength
                    continuation.resume(blob)
                } else {
                    continuation.resumeWithException(MissingResourceException(path))
                }
                null
            }
            req.send(null)
        }
    }
}

internal actual class MissingResourceException actual constructor(path: String) :
    Exception("Missing resource with path: $path")

internal actual fun isSyncResourceLoadingSupported(): Boolean = false

@OptIn(ExperimentalResourceApi::class)
internal actual fun Resource.readBytesSync(): ByteArray = throw UnsupportedOperationException()

internal actual fun parseXML(byteArray: ByteArray): Element {
    throw UnsupportedOperationException("XML Vector Drawables are not supported for Web target")
}


private fun ArrayBuffer.toByteArray(): ByteArray {
    val source = Int8Array(this, 0, byteLength)
    return jsInt8ArrayToKotlinByteArray(source)
}

@JsFun(
    """ (src, size, dstAddr) => {
        const mem8 = new Int8Array(wasmExports.memory.buffer, dstAddr, size);
        mem8.set(src);
    }
"""
)
internal external fun jsExportInt8ArrayToWasm(src: Int8Array, size: Int, dstAddr: Int)

internal fun jsInt8ArrayToKotlinByteArray(x: Int8Array): ByteArray {
    val size = x.length

    @OptIn(UnsafeWasmMemoryApi::class)
    return withScopedMemoryAllocator { allocator ->
        val memBuffer = allocator.allocate(size)
        val dstAddress = memBuffer.address.toInt()
        jsExportInt8ArrayToWasm(x, size, dstAddress)
        ByteArray(size) { i -> (memBuffer + i).loadByte() }
    }
}
