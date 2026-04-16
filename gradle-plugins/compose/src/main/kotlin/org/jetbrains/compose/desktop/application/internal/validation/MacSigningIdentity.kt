/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal.validation

/**
 * Current Apple certificate names plus legacy aliases we still need to support.
 *
 * Legacy compatibility matters because:
 * - jpackage still recognizes the older "3rd Party Mac Developer ..." names
 * - existing user keychains may still contain legacy certificates
 */
internal enum class MacSigningCertificateKind(
    val prefix: String,
    val isAppSigningCertificate: Boolean,
    val isJPackageCompatible: Boolean
) {
    // Current outside-App-Store distribution certificates
    DeveloperIdApplication(
        prefix = "Developer ID Application: ",
        isAppSigningCertificate = true,
        isJPackageCompatible = true
    ),
    DeveloperIdInstaller(
        prefix = "Developer ID Installer: ",
        isAppSigningCertificate = false,
        isJPackageCompatible = false
    ),

    // Current App Store / distribution certificates
    AppleDistribution(
        prefix = "Apple Distribution: ",
        isAppSigningCertificate = true,
        isJPackageCompatible = false
    ),
    MacAppDistribution(
        prefix = "Mac App Distribution: ",
        isAppSigningCertificate = true,
        isJPackageCompatible = false
    ),
    MacInstallerDistribution(
        prefix = "Mac Installer Distribution: ",
        isAppSigningCertificate = false,
        isJPackageCompatible = false
    ),

    // Current development certificates
    AppleDevelopment(
        prefix = "Apple Development: ",
        isAppSigningCertificate = true,
        isJPackageCompatible = false
    ),
    MacDevelopment(
        prefix = "Mac Development: ",
        isAppSigningCertificate = true,
        isJPackageCompatible = false
    ),

    // Legacy compatibility certificates
    ThirdPartyMacDeveloperApplication(
        prefix = "3rd Party Mac Developer Application: ",
        isAppSigningCertificate = true,
        isJPackageCompatible = true
    ),
    ThirdPartyMacDeveloperInstaller(
        prefix = "3rd Party Mac Developer Installer: ",
        isAppSigningCertificate = false,
        isJPackageCompatible = false
    );

    val displayName: String
        get() = prefix.removeSuffix(": ")

    companion object {
        val appSigningKinds = listOf(
            DeveloperIdApplication,
            ThirdPartyMacDeveloperApplication,
            AppleDistribution,
            MacAppDistribution,
            AppleDevelopment,
            MacDevelopment,
        )

        fun fromIdentity(identity: String): MacSigningCertificateKind? =
            entries.firstOrNull { identity.startsWith(it.prefix) }
    }
}

internal data class MacSigningIdentityInput(
    val rawIdentity: String,
    val kind: MacSigningCertificateKind?,
    val name: String
) {
    val fullIdentity: String
        get() = kind?.prefix?.plus(name) ?: rawIdentity

    val isAppSigningIdentity: Boolean
        get() = kind?.isAppSigningCertificate != false

    fun appSigningSearchIdentities(): List<String> {
        if (kind != null) {
            return listOfNotNull(fullIdentity.takeIf { isAppSigningIdentity })
        }

        return MacSigningCertificateKind.appSigningKinds.map { it.prefix + name }
    }

    companion object {
        fun parse(identity: String): MacSigningIdentityInput {
            val kind = MacSigningCertificateKind.fromIdentity(identity)
            val name = kind?.let { identity.removePrefix(it.prefix) } ?: identity
            return MacSigningIdentityInput(
                rawIdentity = identity,
                kind = kind,
                name = name
            )
        }
    }
}

internal data class ResolvedMacSigningIdentity(
    val fullIdentity: String,
    val kind: MacSigningCertificateKind
) {
    val isJPackageCompatible: Boolean
        get() = kind.isJPackageCompatible

    val installerSigningIdentityCandidates: List<String>
        get() = when (kind) {
            MacSigningCertificateKind.DeveloperIdApplication ->
                listOf(fullIdentity.replaceFirst(kind.prefix, MacSigningCertificateKind.DeveloperIdInstaller.prefix))

            MacSigningCertificateKind.ThirdPartyMacDeveloperApplication,
            MacSigningCertificateKind.AppleDistribution,
            MacSigningCertificateKind.MacAppDistribution -> listOf(
                MacSigningCertificateKind.ThirdPartyMacDeveloperInstaller.prefix + commonName,
                MacSigningCertificateKind.MacInstallerDistribution.prefix + commonName,
            )

            MacSigningCertificateKind.AppleDevelopment,
            MacSigningCertificateKind.MacDevelopment,
            MacSigningCertificateKind.DeveloperIdInstaller,
            MacSigningCertificateKind.ThirdPartyMacDeveloperInstaller,
            MacSigningCertificateKind.MacInstallerDistribution -> emptyList()
        }

    private val commonName: String
        get() = fullIdentity.removePrefix(kind.prefix)

    companion object {
        fun fromIdentity(identity: String): ResolvedMacSigningIdentity {
            val kind = MacSigningCertificateKind.fromIdentity(identity)
            check(kind != null && kind.isAppSigningCertificate) {
                "Unsupported macOS app signing identity: '$identity'"
            }
            return ResolvedMacSigningIdentity(identity, kind)
        }
    }
}
