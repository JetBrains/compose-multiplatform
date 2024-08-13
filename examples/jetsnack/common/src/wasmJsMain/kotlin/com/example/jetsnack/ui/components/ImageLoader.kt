package com.example.jetsnack.ui.components

import org.jetbrains.skia.ExternalSymbolName
import org.jetbrains.skia.impl.NativePointer
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.xhr.XMLHttpRequest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

private class MissingResourceException(url: String): Exception("GET $url failed")

suspend fun loadImage(url: String): ArrayBuffer {
    return suspendCoroutine { continuation ->
        val req = XMLHttpRequest()
        req.open("GET", url, true)
        req.responseType = "arraybuffer".toJsString().unsafeCast()

        req.onload = { _ ->
            val arrayBuffer = req.response
            if (arrayBuffer is ArrayBuffer) {
                continuation.resume(arrayBuffer)
            } else {
                continuation.resumeWithException(MissingResourceException(url))
            }
        }
        req.send("")
    }
}

fun ArrayBuffer.toByteArray(): ByteArray {
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

@ExternalSymbolName("_malloc")
@kotlin.wasm.WasmImport("skia", "malloc")
private external fun _malloc(size: Int): NativePointer

@ExternalSymbolName("_free")
@kotlin.wasm.WasmImport("skia", "free")
private external fun _free(ptr: NativePointer)