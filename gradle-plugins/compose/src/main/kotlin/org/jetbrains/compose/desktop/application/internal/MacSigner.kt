/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.jetbrains.compose.desktop.application.internal.validation.ValidatedMacOSSigningSettings
import java.io.File
import java.nio.file.Files
import java.util.regex.Pattern

internal class MacSigner(
    val settings: ValidatedMacOSSigningSettings,
    private val runExternalTool: ExternalToolRunner
) {
    private lateinit var signKey: String

    init {
        runExternalTool(
            MacUtils.security,
            args = listOfNotNull(
                "find-certificate",
                "-a",
                "-c",
                settings.fullDeveloperID,
                settings.keychain?.absolutePath
            ),
            processStdout = { stdout ->
                signKey = findCertificate(stdout)
            }
        )
    }

    /**
     * If [entitlements] file is provided, executables are signed with entitlements.
     * Set [forceEntitlements] to `true` to sign all types of files with the provided [entitlements].
     */
    fun sign(
        file: File,
        entitlements: File? = null,
        forceEntitlements: Boolean = false
    ) {
        val args = arrayListOf(
            "-vvvv",
            "--timestamp",
            "--options", "runtime",
            "--force",
            "--prefix", settings.prefix,
            "--sign", signKey
        )

        settings.keychain?.let {
            args.add("--keychain")
            args.add(it.absolutePath)
        }

        if (forceEntitlements || Files.isExecutable(file.toPath())) {
            entitlements?.let {
                args.add("--entitlements")
                args.add(it.absolutePath)
            }
        }

        args.add(file.absolutePath)

        runExternalTool(MacUtils.codesign, args)
    }

    fun unsign(file: File) {
        val args = listOf(
            "-vvvv",
            "--remove-signature",
            file.absolutePath
        )
        runExternalTool(MacUtils.codesign, args)

    }

    private fun findCertificate(certificates: String): String {
        val regex = Pattern.compile("\"alis\"<blob>=\"([^\"]+)\"")
        val m = regex.matcher(certificates)
        if (!m.find()) {
            val keychainPath = settings.keychain?.absolutePath
            error(
                "Could not find certificate for '${settings.identity}'" +
                        " in keychain [${keychainPath.orEmpty()}]"
            )
        }

        val result = m.group(1)
        if (m.find())
            error(
                "Multiple matching certificates are found for '${settings.fullDeveloperID}'. " +
                "Please specify keychain containing unique matching certificate."
            )
        return result
    }
}