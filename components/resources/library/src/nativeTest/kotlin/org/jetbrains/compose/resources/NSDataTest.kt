package org.jetbrains.compose.resources

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.closeFile
import platform.Foundation.create
import platform.Foundation.fileHandleForReadingAtPath
import platform.Foundation.readDataOfLength
import platform.Foundation.seekToFileOffset
import platform.Foundation.writeToFile
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(BetaInteropApi::class)
class NSDataTest {
    @Test
    fun emptyDataReturnsEmptyArray() {
        // Foundation returns empty data for empty files, zero-length reads and reads at EOF.
        assertContentEquals(byteArrayOf(), NSData.create(bytes = null, length = 0u).toByteArray())
    }

    @Test
    fun nonEmptyDataPreservesAllBytes() {
        val expected = byteArrayOf(0, 1, 127, -128, -1)
        val data = expected.usePinned {
            NSData.create(bytes = it.addressOf(0), length = expected.size.toULong())
        }
        assertContentEquals(expected, data.toByteArray())
    }

    @Test
    fun emptyFileReturnsEmptyArray() = withDataFile(byteArrayOf()) { path ->
        val data = assertNotNull(NSFileManager.defaultManager.contentsAtPath(path))
        assertContentEquals(byteArrayOf(), data.toByteArray())
    }

    @Test
    fun zeroLengthReadReturnsEmptyArray() = withDataFile(byteArrayOf(42)) { path ->
        withFileHandle(path) { handle ->
            assertContentEquals(byteArrayOf(), handle.readDataOfLength(0u).toByteArray())
        }
    }

    @Test
    fun readAtEndOfFileReturnsEmptyArray() = withDataFile(byteArrayOf(42)) { path ->
        withFileHandle(path) { handle ->
            handle.seekToFileOffset(1u)
            assertContentEquals(byteArrayOf(), handle.readDataOfLength(1u).toByteArray())
        }
    }

    private fun withDataFile(bytes: ByteArray, block: (String) -> Unit) {
        val path = NSTemporaryDirectory() + "compose-resource-" + NSUUID().UUIDString
        val data = if (bytes.isEmpty()) {
            NSData.create(bytes = null, length = 0u)
        } else {
            bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        }
        assertTrue(data.writeToFile(path, atomically = true))
        try {
            block(path)
        } finally {
            NSFileManager.defaultManager.removeItemAtPath(path, error = null)
        }
    }

    private fun withFileHandle(path: String, block: (NSFileHandle) -> Unit) {
        val handle = assertNotNull(NSFileHandle.fileHandleForReadingAtPath(path))
        try {
            block(handle)
        } finally {
            handle.closeFile()
        }
    }
}
