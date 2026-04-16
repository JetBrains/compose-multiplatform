/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.jetbrains.compose.desktop.application.internal.validation.MacSigningCertificateKind
import org.jetbrains.compose.desktop.application.internal.validation.ResolvedMacSigningIdentity
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

    open val resolvedSigningIdentity: ResolvedMacSigningIdentity?
        get() = null
}

internal class NoCertificateSigner(runTool: ExternalToolRunner) : MacSigner(runTool) {
    override fun sign(file: File, entitlements: File?, forceEntitlements: Boolean) {
        unsign(file)
        if (currentArch == Arch.Arm64) {
            // Apple Silicon requires binaries to be signed
            // For local builds, ad hoc signatures are OK
            // https://wiki.lazarus.freepascal.org/Code_Signing_for_macOS
            val args = arrayListOf("-vvvv", "--sign", "-", "--options", "runtime", "--force")
            entitlements?.let {
                args.add("--entitlements")
                args.add(entitlements.absolutePath)
            }
            args.add(file.absolutePath)
            runTool.codesign(*args.toTypedArray())
        }
    }

    override val settings: ValidatedMacOSSigningSettings?
        get() = null
}

internal class MacSignerImpl(
    override val settings: ValidatedMacOSSigningSettings,
    runTool: ExternalToolRunner
) : MacSigner(runTool) {
    @Transient
    private var signKeyValue: String? = null

    override val resolvedSigningIdentity: ResolvedMacSigningIdentity by lazy {
        resolveMacSigningIdentity(settings) { candidate ->
            var certificates = ""
            runTool(
                MacUtils.security,
                args = listOfNotNull(
                    "find-certificate",
                    "-a",
                    "-c",
                    candidate,
                    settings.keychain?.absolutePath
                ),
                processStdout = { certificates = it },
                logToConsole = ExternalToolRunner.LogToConsole.Never
            )
            certificates
        }
    }

    override fun sign(
        file: File,
        entitlements: File?,
        forceEntitlements: Boolean
    ) {
        // sign key calculation is delayed to avoid
        // creating an external process during the configuration
        // phase, which became an error in Gradle 8.1
        // https://github.com/JetBrains/compose-multiplatform/issues/3060
        val signKey = signKeyValue ?: resolvedSigningIdentity.fullIdentity.also { signKeyValue = it }
        runTool.unsign(file)
        runTool.sign(
            file = file,
            signKey = signKey,
            entitlements = entitlements?.takeIf { forceEntitlements || file.isExecutable },
            prefix = settings.prefix,
            keychain = settings.keychain
        )
    }
}

internal fun resolveMacSigningIdentity(
    settings: ValidatedMacOSSigningSettings,
    findCertificates: (String) -> String
): ResolvedMacSigningIdentity {
    val parsedKind = settings.parsedIdentity.kind
    if (parsedKind != null) {
        check(parsedKind.isAppSigningCertificate) {
            buildString {
                append("Signing settings error: '${settings.identity}' is not an app signing certificate. ")
                append("Use one of: ")
                append(MacSigningCertificateKind.appSigningKinds.joinToString { it.displayName })
                append(".")
            }
        }
    }

    val matches = mutableListOf<String>()
    for (candidate in settings.appSigningSearchIdentities) {
        matches += extractCertificateAliases(findCertificates(candidate))
            .filter { matchesCandidateIdentity(it, candidate) }
    }

    if (matches.isEmpty()) {
        error(buildMissingCertificateMessage(settings))
    }

    val distinctMatches = matches.distinct()
    if (distinctMatches.size > 1) {
        error(buildAmbiguousCertificateMessage(settings, distinctMatches.toSet()))
    }

    return MacSigningCertificateKind.resolveIdentity(distinctMatches.single())
}

private fun buildMissingCertificateMessage(settings: ValidatedMacOSSigningSettings): String {
    val keychainPath = settings.keychain?.absolutePath.orEmpty()
    val identity = settings.identity
    val checkedIdentities = settings.appSigningSearchIdentities.joinToString("\n") { "* $it" }
    return buildString {
        appendLine("Could not find a matching app signing certificate for '$identity' in keychain [$keychainPath].")
        appendLine("Checked certificate names:")
        appendLine(checkedIdentities)
        append("For notarized distribution outside the App Store, use 'Developer ID Application'. ")
        append("For Mac App Store uploads, use 'Mac App Distribution' or 'Apple Distribution' for the app ")
        append("and a matching installer certificate for PKG. Development certificates such as ")
        append("'Apple Development', 'Mac Development', and 'Mac Developer' are only suitable for local app signing.")
    }
}

private fun buildAmbiguousCertificateMessage(
    settings: ValidatedMacOSSigningSettings,
    matches: Set<String>
): String = buildString {
    appendLine("Multiple matching certificates are found for '${settings.identity}' in keychain [${settings.keychain?.absolutePath.orEmpty()}].")
    appendLine("Matching certificates:")
    appendLine(matches.joinToString("\n") { "* $it" })
    append("Specify the full certificate identity in 'nativeDistributions.macOS.signing.identity'.")
}

private fun matchesCandidateIdentity(alias: String, candidate: String): Boolean {
    val candidateIdentity = MacSigningCertificateKind.parseIdentity(candidate)
    val aliasIdentity = MacSigningCertificateKind.parseIdentity(alias)
    if (candidateIdentity.kind == null || aliasIdentity.kind != candidateIdentity.kind) {
        return false
    }

    val candidateName = candidateIdentity.name
    val aliasName = aliasIdentity.name
    if (aliasName == candidateName) {
        return true
    }
    if (!aliasName.startsWith(candidateName)) {
        return false
    }
    return TEAM_ID_SUFFIX_REGEX.matches(aliasName.removePrefix(candidateName))
}

internal fun extractCertificateAliases(certificates: String): List<String> {
    // When the developer id contains non-ascii characters, the output of `security find-certificate` is
    // slightly different. The `alis` line first has the hex-encoded developer id, then some spaces,
    // and then the developer id with non-ascii characters encoded as octal.
    // See https://bugs.openjdk.org/browse/JDK-8308042
    val m = CERTIFICATE_ALIAS_REGEX.matcher(certificates)
    val result = linkedSetOf<String>()
    while (m.find()) {
        val hexEncoded = m.group(1)
        val alias = if (hexEncoded.isNullOrBlank()) {
            m.group(2)
        } else {
            hexEncoded
                .substring(2)
                .chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
                .toString(Charsets.UTF_8)
        }
        result.add(alias)
    }
    return result.toList()
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

private val CERTIFICATE_ALIAS_REGEX: Pattern =
    Pattern.compile("\"alis\"<blob>=(0x[0-9A-F]+)?\\s*\"([^\"]+)\"")

private val TEAM_ID_SUFFIX_REGEX = Regex(""" \([A-Z0-9]{10}\)$""")
