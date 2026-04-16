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
    // Outside-App-Store distribution
    DeveloperIdApplication(prefix = "Developer ID Application: ", isAppSigningCertificate = true, isJPackageCompatible = true),
    DeveloperIdInstaller(prefix = "Developer ID Installer: ", isAppSigningCertificate = false, isJPackageCompatible = false),
    // App Store / distribution
    AppleDistribution(prefix = "Apple Distribution: ", isAppSigningCertificate = true, isJPackageCompatible = false),
    // Development
    AppleDevelopment(prefix = "Apple Development: ", isAppSigningCertificate = true, isJPackageCompatible = false),
    // Legacy (still issued by Apple under these CNs despite portal UI showing different names)
    ThirdPartyMacDeveloperApplication(prefix = "3rd Party Mac Developer Application: ", isAppSigningCertificate = true, isJPackageCompatible = true),
    ThirdPartyMacDeveloperInstaller(prefix = "3rd Party Mac Developer Installer: ", isAppSigningCertificate = false, isJPackageCompatible = false),
    MacDeveloper(prefix = "Mac Developer: ", isAppSigningCertificate = true, isJPackageCompatible = false);

    val displayName: String
        get() = prefix.removeSuffix(": ")

    /** Installer certificate kinds that pair with this app signing certificate. */
    val installerKinds: List<MacSigningCertificateKind>
        get() = when (this) {
            DeveloperIdApplication -> listOf(DeveloperIdInstaller)
            ThirdPartyMacDeveloperApplication, AppleDistribution ->
                listOf(ThirdPartyMacDeveloperInstaller)
            AppleDevelopment, MacDeveloper, DeveloperIdInstaller,
            ThirdPartyMacDeveloperInstaller -> emptyList()
        }

    companion object {
        val appSigningKinds: List<MacSigningCertificateKind>
            get() = entries.filter { it.isAppSigningCertificate }

        private fun fromIdentity(identity: String): MacSigningCertificateKind? =
            entries.firstOrNull { identity.startsWith(it.prefix) }

        fun parseIdentity(identity: String): ParsedSigningIdentity {
            val kind = fromIdentity(identity)
            val name = kind?.let { identity.removePrefix(it.prefix) } ?: identity
            return ParsedSigningIdentity(kind, name)
        }

        fun resolveIdentity(identity: String): ResolvedMacSigningIdentity {
            val kind = fromIdentity(identity)
            check(kind != null && kind.isAppSigningCertificate) {
                "Unsupported macOS app signing identity: '$identity'"
            }
            return ResolvedMacSigningIdentity(identity, kind)
        }
    }
}

internal data class ParsedSigningIdentity(
    val kind: MacSigningCertificateKind?,
    val name: String
)

internal data class ResolvedMacSigningIdentity(
    val fullIdentity: String,
    val kind: MacSigningCertificateKind
) {
    val isJPackageCompatible: Boolean
        get() = kind.isJPackageCompatible

    val installerSigningIdentityCandidates: List<String>
        get() {
            val commonName = fullIdentity.removePrefix(kind.prefix)
            return kind.installerKinds.map { it.prefix + commonName }
        }
}
