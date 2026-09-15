package org.jetbrains.compose.resources

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

@OptIn(UnsafeWasmMemoryApi::class)
internal fun fastArrayBufferToByteArray(arrayBuffer: ArrayBuffer): ByteArray {
    if (isWasmExportsAvailable()) {
        val size = arrayBuffer.byteLength
        return withScopedMemoryAllocator { allocator ->
            val bufferPtr = allocator.allocate(size)
            copyArrayBufferToWasmMemory(arrayBuffer, bufferPtr.address.toInt())
            readFromLinearMemory(bufferPtr, 0, size)
        }
    } else {
        // slow fallback. use WebAssembly.Memory when CMP is migrated to Kotlin 2.4.20
        // see https://youtrack.jetbrains.com/issue/CMP-10801
        return Int8Array(arrayBuffer).toByteArray()
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun isWasmExportsAvailable(): Boolean = js("typeof wasmExports !== 'undefined'")

//language=js
@OptIn(ExperimentalWasmJsInterop::class)
private fun copyArrayBufferToWasmMemory(ab: ArrayBuffer, ptr: Int): Unit = js("""{
      const data = new Uint8Array(ab);
      new Uint8Array(wasmExports.memory.buffer).set(data, ptr);
}""")

@OptIn(UnsafeWasmMemoryApi::class)
private fun readFromLinearMemory(base: Pointer, offset: Int, length: Int): ByteArray {
    val bytes = ByteArray(length)
    val src = base + offset
    val intCount = length / 4
    var idx = 0
    for (i in 0 until intCount) {
        val value = (src + idx).loadInt()
        bytes[idx] = (value and 0xFF).toByte()
        bytes[idx + 1] = ((value shr 8) and 0xFF).toByte()
        bytes[idx + 2] = ((value shr 16) and 0xFF).toByte()
        bytes[idx + 3] = ((value shr 24) and 0xFF).toByte()
        idx += 4
    }
    for (i in idx until length) {
        bytes[i] = (src + i).loadByte()
    }
    return bytes
}