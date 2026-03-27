/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.publishing.utils

import java.io.File
import java.security.MessageDigest

internal fun defaultChecksums(): Checksum = CompositeChecksum(
    BasicChecksum("MD5", ".md5"),
    BasicChecksum("SHA-1", ".sha1"),
    BasicChecksum("SHA-256", ".sha256"),
    BasicChecksum("SHA-512", ".sha512"),
)

internal abstract class Checksum {
    abstract fun update(input: ByteArray)
    abstract fun reset()
    abstract fun write(basePath: String)
    abstract fun isChecksumFile(file: File): Boolean

    fun generateChecksumFilesFor(file: File) {
        reset()
        update(file.readBytes())
        write(basePath = file.path)
    }
}

private class CompositeChecksum(private vararg val checksums: Checksum) : Checksum() {
    override fun update(input: ByteArray) {
        checksums.forEach { it.update(input) }
    }

    override fun reset() {
        checksums.forEach { it.reset() }
    }

    override fun write(basePath: String) {
        checksums.forEach { it.write(basePath) }
    }

    override fun isChecksumFile(file: File): Boolean =
        checksums.any { it.isChecksumFile(file) }
}

private class BasicChecksum(
    private val md: MessageDigest,
    private val checksumExt: String
) : Checksum() {
    constructor(algorithm: String, extension: String) : this(MessageDigest.getInstance(algorithm), extension)

    override fun update(input: ByteArray) {
        md.update(input)
    }

    override fun reset() {
        md.reset()
    }

    override fun write(basePath: String) {
        File(basePath + checksumExt).writeHexString(md.digest())
    }

    override fun isChecksumFile(file: File): Boolean =
        file.name.endsWith(checksumExt, ignoreCase = true)

    private fun File.writeHexString(bytes: ByteArray) {
        bufferedWriter().use { writer ->
            for (b in bytes) {
                val hex = Integer.toHexString(0xFF and b.toInt())
                if (hex.length == 1) {
                    writer.append('0')
                }
                writer.append(hex)
            }
        }
    }
}