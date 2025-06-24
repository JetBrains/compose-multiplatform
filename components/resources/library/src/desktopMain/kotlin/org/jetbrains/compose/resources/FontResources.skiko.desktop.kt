package org.jetbrains.compose.resources

import java.util.zip.CRC32

internal actual fun checksum(data: ByteArray): Long {
    val crc = CRC32()
    crc.update(data)
    return crc.value
}