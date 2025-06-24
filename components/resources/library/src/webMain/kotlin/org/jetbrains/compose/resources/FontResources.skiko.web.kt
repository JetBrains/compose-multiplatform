package org.jetbrains.compose.resources

internal actual fun checksum(data: ByteArray): Long = data.size.toLong()