package org.jetbrains.compose.desktop.application.internal.files

import java.io.File
import java.security.DigestInputStream
import java.security.MessageDigest

internal fun fileHash(file: File): String {
    val md5 = MessageDigest.getInstance("MD5")
    file.inputStream().buffered().use { fis ->
        DigestInputStream(fis, md5).use { ds ->
            while (ds.read() != -1) {}
        }
    }
    val digest = md5.digest()
    return buildString(digest.size * 2) {
        for (byte in digest) {
            append(Integer.toHexString(0xFF and byte.toInt()))
        }
    }
}