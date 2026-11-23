package org.jetbrains.compose.resources

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.posix.memcpy

internal fun NSData.toByteArray(): ByteArray = ByteArray(length.toInt()).apply {
    if (isNotEmpty()) {
        usePinned { memcpy(it.addressOf(0), bytes, length) }
    }
}
