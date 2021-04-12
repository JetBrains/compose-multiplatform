/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal.files

import org.gradle.process.ExecOperations
import org.jetbrains.compose.desktop.application.internal.MacUtils
import org.jetbrains.compose.desktop.application.internal.isJarFile
import org.jetbrains.compose.desktop.application.internal.validation.ValidatedMacOSSigningSettings
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal class MacJarSignFileCopyingProcessor(
    private val tempDir: File,
    private val execOperations: ExecOperations,
    private val signing: ValidatedMacOSSigningSettings
) : FileCopyingProcessor {
    private val signKey: String

    init {
        val certificates = ByteArrayOutputStream().use { baos ->
            PrintStream(baos).use { ps ->
                execOperations.exec { exec ->
                    exec.executable = MacUtils.security.absolutePath
                    val args = arrayListOf("find-certificate", "-a", "-c", signing.fullDeveloperID)
                    signing.keychain?.let { args.add(it.absolutePath) }
                    exec.args(*args.toTypedArray())
                    exec.standardOutput = ps
                }
            }
            baos.toString()
        }
        val regex = Pattern.compile("\"alis\"<blob>=\"([^\"]+)\"")
        val m = regex.matcher(certificates)
        if (!m.find()) {
            val keychainPath = signing.keychain?.absolutePath
            error(
                "Could not find certificate for '${signing.identity}'" +
                        " in keychain [${keychainPath.orEmpty()}]"
            )
        }

        signKey = m.group(1)
        if (m.find()) error("Multiple matching certificates are found for '${signing.fullDeveloperID}'. " +
                "Please specify keychain containing unique matching certificate.")
    }

    override fun copy(source: File, target: File) {
        if (source.isJarFile) {
            signNativeLibsInJar(source, target)
        } else {
            SimpleFileCopyingProcessor.copy(source, target)
            if (source.name.isDylibPath) {
                signDylib(target)
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
            signDylib(unpackedDylibFile)
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

    private fun signDylib(dylibFile: File) {
        val args = arrayListOf(
            "-vvvv",
            "--timestamp",
            "--options", "runtime",
            "--force",
            "--prefix", signing.prefix,
            "--sign", signKey
        )

        signing.keychain?.let {
            args.add("--keychain")
            args.add(it.absolutePath)
        }

        args.add(dylibFile.absolutePath)

        execOperations.exec { exec ->
            exec.executable = MacUtils.codesign.absolutePath
            exec.args(*args.toTypedArray())
        }.assertNormalExitValue()
    }
}

private val String.isDylibPath
    get() = endsWith(".dylib")