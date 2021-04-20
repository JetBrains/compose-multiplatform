/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal.files

import org.jetbrains.compose.desktop.application.internal.MacSigner
import org.jetbrains.compose.desktop.application.internal.isJarFile
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal class MacJarSignFileCopyingProcessor(
    private val signer: MacSigner,
    private val tempDir: File,
) : FileCopyingProcessor {
    override fun copy(source: File, target: File) {
        if (source.isJarFile) {
            signNativeLibsInJar(source, target)
        } else {
            SimpleFileCopyingProcessor.copy(source, target)
            if (source.name.isDylibPath) {
                signer.sign(target)
            }
        }
    }

    private fun signNativeLibsInJar(source: File, target: File) {
        if (target.exists()) target.delete()

        transformJar(source, target) { zin, zout, entry ->
            if (entry.name.isDylibPath) {
                signDylibEntry(zin, zout, entry)
            } else {
                zout.withNewEntry(ZipEntry(entry)) {
                    zin.copyTo(zout)
                }
            }
        }
    }

    private fun signDylibEntry(zin: ZipInputStream, zout: ZipOutputStream, sourceEntry: ZipEntry) {
        val unpackedDylibFile = tempDir.resolve(sourceEntry.name.substringAfterLast("/"))
        try {
            zin.copyTo(unpackedDylibFile)
            signer.sign(unpackedDylibFile)
            val targetEntry = ZipEntry(sourceEntry.name).apply {
                comment = sourceEntry.comment
                extra = sourceEntry.extra
                method = sourceEntry.method
                size = unpackedDylibFile.length()
            }
            zout.withNewEntry(ZipEntry(targetEntry)) {
                unpackedDylibFile.copyTo(zout)
            }
        } finally {
            unpackedDylibFile.delete()
        }
    }
}

private val String.isDylibPath
    get() = endsWith(".dylib")