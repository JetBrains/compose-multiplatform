/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.jetbrains.compose.desktop.application.internal.validation.ValidatedMacOSSigningSettings
import org.jetbrains.compose.internal.utils.Arch
import org.jetbrains.compose.internal.utils.MacUtils
import org.jetbrains.compose.internal.utils.currentArch
import java.io.File
import java.util.regex.Pattern
import kotlin.io.path.isExecutable

internal abstract class MacSigner(protected val runTool: ExternalToolRunner) {
    /**
     * If [entitlements] file is provided, executables are signed with entitlements.
     * Set [forceEntitlements] to `true` to sign all types of files with the provided [entitlements].
     */
    abstract fun sign(
        file: File,
        entitlements: File? = null,
        forceEntitlements: Boolean = false
    )

    fun unsign(file: File) {
        runTool.unsign(file)
    }

    abstract val settings: ValidatedMacOSSigningSettings?
}

internal class NoCertificateSigner(runTool: ExternalToolRunner) : MacSigner(runTool) {
    override fun sign(file: File, entitlements: File?, forceEntitlements: Boolean) {
        unsign(file)
        if (currentArch == Arch.Arm64) {
            // Apple Silicon requires binaries to be signed
            // For local builds, ad hoc signatures are OK
            // https://wiki.lazarus.freepascal.org/Code_Signing_for_macOS
            runTool.codesign("--sign", "-", "-vvvv", file.absolutePath)
        }
    }

    override val settings: ValidatedMacOSSigningSettings?
        get() = null
}

internal class MacSignerImpl(
    override val settings: ValidatedMacOSSigningSettings,
    runTool: ExternalToolRunner
) : MacSigner(runTool)  {
    private lateinit var signKey: String

    init {
        runTool(
            MacUtils.security,
            args = listOfNotNull(
                "find-certificate",
                "-a",
                "-c",
                settings.fullDeveloperID,
                settings.keychain?.absolutePath
            ),
            processStdout = { signKey = matchCertificates(it) }
        )
    }

    override fun sign(
        file: File,
        entitlements: File?,
        forceEntitlements: Boolean
    ) {
        runTool.unsign(file)
        runTool.sign(
            file = file,
            signKey = signKey,
            entitlements = entitlements?.takeIf { forceEntitlements || file.isExecutable },
            prefix = settings.prefix,
            keychain = settings.keychain
        )
    }

    private fun matchCertificates(certificates: String): String {
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

private fun ExternalToolRunner.codesign(vararg args: String) =
    this(MacUtils.codesign, args.toList())

private fun ExternalToolRunner.unsign(file: File) =
    codesign("-vvvv", "--remove-signature", file.absolutePath)

private fun ExternalToolRunner.sign(
    file: File,
    signKey: String,
    entitlements: File?,
    prefix: String?,
    keychain: File?
) = codesign(
    "-vvvv",
    "--timestamp",
    "--options", "runtime",
    "--force",
    *optionalArg("--prefix", prefix),
    "--sign", signKey,
    *optionalArg("--keychain", keychain?.absolutePath),
    *optionalArg("--entitlements", entitlements?.absolutePath),
    file.absolutePath
)

private fun optionalArg(arg: String, value: String?): Array<String> =
    if (value != null) arrayOf(arg, value) else emptyArray()

private val File.isExecutable: Boolean
    get() = toPath().isExecutable()