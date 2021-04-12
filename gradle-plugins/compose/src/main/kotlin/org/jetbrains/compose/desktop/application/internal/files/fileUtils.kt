/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal.files

import java.io.*
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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

internal inline fun transformJar(
    sourceJar: File,
    targetJar: File,
    fn: (zin: ZipInputStream, zout: ZipOutputStream, entry: ZipEntry) -> Unit
) {
    ZipInputStream(FileInputStream(sourceJar).buffered()).use { zin ->
        ZipOutputStream(FileOutputStream(targetJar).buffered()).use { zout ->
            for (sourceEntry in generateSequence { zin.nextEntry }) {
                fn(zin, zout, sourceEntry)
            }
        }
    }
}

internal inline fun ZipOutputStream.withNewEntry(zipEntry: ZipEntry, fn: () -> Unit) {
    putNextEntry(zipEntry)
    fn()
    closeEntry()
}

internal fun InputStream.copyTo(file: File) {
    file.outputStream().buffered().use { os ->
        copyTo(os)
    }
}

internal fun File.copyTo(os: OutputStream) {
    inputStream().buffered().use { bis ->
        bis.copyTo(os)
    }
}