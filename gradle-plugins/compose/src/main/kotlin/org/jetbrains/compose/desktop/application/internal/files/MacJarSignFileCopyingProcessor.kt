/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal.files

import org.jetbrains.compose.desktop.application.internal.MacSigner
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal class MacJarSignFileCopyingProcessor(
    private val signer: MacSigner,
    private val tempDir: File,
    private val jvmRuntimeVersion: Int
) : FileCopyingProcessor {
    override fun copy(source: File, target: File) {
        if (source.isJarFile) {
            signNativeLibsInJar(source, target)
        } else {
            SimpleFileCopyingProcessor.copy(source, target)
            if (source.name.isDylibPath) {
                when {
                    jvmRuntimeVersion < 17 -> signer.sign(target)
                    /**
                     * JDK 17 started to sign non-jar dylibs,
                     * but it fails, when libs are already signed,
                     * so we need to remove signature before running jpackage.
                     *
                     * JDK 18 processes signed libraries fine, so we don't have to do anything.
                     *
                     * Note that the JDK only signs dylib files and not jnilib files,
                     * so jnilib files still need to be signed here.
                     */
                    jvmRuntimeVersion == 17 -> {
                        if (source.name.endsWith(".jnilib")) {
                            signer.sign(target)
                        } else {
                            signer.unsign(target)
                        }
                    }
                    else -> {
                        if (source.name.endsWith(".jnilib")) {
                            signer.sign(target)
                        }
                    }
                }
            }
        }
    }

    private fun signNativeLibsInJar(source: File, target: File) {
        if (target.exists()) target.delete()

        transformJar(source, target) { entry, zin, zout ->
            if (entry.name.isDylibPath) {
                signDylibEntry(entry, zin, zout)
            } else {
                copyZipEntry(entry, zin, zout)
            }
        }
    }

    private fun signDylibEntry(sourceEntry: ZipEntry, zin: ZipInputStream, zout: ZipOutputStream) {
        val unpackedDylibFile = tempDir.resolve(sourceEntry.name.substringAfterLast("/"))
        try {
            zin.copyTo(unpackedDylibFile)
            signer.sign(unpackedDylibFile)
            unpackedDylibFile.inputStream().buffered().use {
                copyZipEntry(sourceEntry, from = it, to = zout)
            }
        } finally {
            unpackedDylibFile.delete()
        }
    }
}

internal val String.isDylibPath
    get() = endsWith(".dylib") || endsWith(".jnilib")
