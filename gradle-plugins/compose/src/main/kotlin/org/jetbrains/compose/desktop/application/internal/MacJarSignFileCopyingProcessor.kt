package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.provider.Property
import org.gradle.process.ExecOperations
import org.jetbrains.compose.desktop.application.dsl.MacOSSigningSettings
import java.io.*
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal class MacJarSignFileCopyingProcessor(
    private val tempDir: File,
    private val execOperations: ExecOperations,
    macBundleID: Property<String?>,
    signSettings: MacOSSigningSettings,
) : FileCopyingProcessor {
    private val bundleId = macBundleID.orNull
    private val keychainPath = signSettings.keychain
    private val signPrefix: String
    private val signKey: String

    init {
        check(currentOS == OS.MacOS) { "$currentOS is not compatible with ${this::class.java}" }
        check(bundleId != null) {
            """|Signing requires to specify unique application's identifier. Specify an identifier using DSL like so:
               |nativeExecutables {
               |  macOS {
               |      bundleID = "com.mycompany.myapp"
               |  }
               |bundleID may only contain alphanumeric characters (A-Z,a-z,0-9), hyphen (-) and period (.) characters
               |""".trimMargin()
        }
        check(bundleId.matches("[A-Za-z0-9\\-\\.]+".toRegex())) {
            "bundleID may only contain alphanumeric characters (A-Z,a-z,0-9), hyphen (-)" +
                    " and period (.) characters"
        }
        signPrefix = signSettings.signPrefix
            ?: (bundleId.substringBeforeLast(".") + ".").takeIf { bundleId.contains('.') }
            ?: error("Could not infer 'signPrefix'. Specify explicitly or use reverse DNS notation for bundleID")

        val identity = signSettings.identity

        val developerIdPrefix = "Developer ID Application: "
        val thirdPartyMacDeveloperPrefix = "3rd Party Mac Developer Application: "
        val signIdentity = when {
            identity.startsWith(developerIdPrefix) -> identity
            identity.startsWith(thirdPartyMacDeveloperPrefix) -> identity
            else -> developerIdPrefix + identity
        }

        val certificates = ByteArrayOutputStream().use { baos ->
            PrintStream(baos).use { ps ->
                execOperations.exec { exec ->
                    exec.executable = MacUtils.security.absolutePath
                    val args = arrayListOf("find-certificate", "-a", "-c", signIdentity)
                    keychainPath?.let { args.add(it) }
                    exec.args(*args.toTypedArray())
                    exec.standardOutput = ps
                }
            }
            baos.toString()
        }
        val regex = Pattern.compile("\"alis\"<blob>=\"([^\"]+)\"")
        val m = regex.matcher(certificates)
        if (!m.find())
            error(
                "Could not find certificate for '$signIdentity'" +
                " in keychain '$keychainPath'".takeIf { keychainPath != null }.orEmpty()
            )

        signKey = m.group(1)
        if (m.find()) error("Multiple matching certificates are found for '$signIdentity'. " +
                "Please specify keychain containing unique matching certificate.")
    }

    override fun copy(source: File, target: File) {
        if (!source.isJarFile) {
            SimpleFileCopyingProcessor.copy(source, target)
            return
        }

        if (target.exists()) target.delete()

        ZipInputStream(FileInputStream(source).buffered()).use { zin ->
            ZipOutputStream(FileOutputStream(target).buffered()).use { zout ->
                copyAndSignNativeLibs(zin, zout)
            }
        }
    }

    private fun copyAndSignNativeLibs(zin: ZipInputStream, zout: ZipOutputStream) {
        for (sourceEntry in generateSequence { zin.nextEntry }) {
            if (!sourceEntry.name.endsWith(".dylib")) {
                zout.putNextEntry(ZipEntry(sourceEntry))
                zin.copyTo(zout)
            } else {

                val unpackedDylibFile = tempDir.resolve(sourceEntry.name.substringAfterLast("/"))
                try {
                    unpackedDylibFile.outputStream().buffered().use {
                        zin.copyTo(it)
                    }

                    signDylib(unpackedDylibFile)
                    val targetEntry = ZipEntry(sourceEntry.name).apply {
                        comment = sourceEntry.comment
                        extra = sourceEntry.extra
                        method = sourceEntry.method
                        size = unpackedDylibFile.length()
                    }
                    zout.putNextEntry(targetEntry)

                    unpackedDylibFile.inputStream().buffered().use {
                        it.copyTo(zout)
                    }
                } finally {
                    unpackedDylibFile.delete()
                }
            }
            zout.closeEntry()
        }
    }

    private fun signDylib(dylibFile: File) {
        val args = arrayListOf(
            "-vvvv",
            "--timestamp",
            "--options", "runtime",
            "--force",
            "--prefix", signPrefix,
            "--sign", signKey
        )

        keychainPath?.let {
            args.add("--keychain")
            args.add(it)
        }

        args.add(dylibFile.absolutePath)

        execOperations.exec { exec ->
            exec.executable = MacUtils.codesign.absolutePath
            exec.args(*args.toTypedArray())
        }.assertNormalExitValue()
    }
}