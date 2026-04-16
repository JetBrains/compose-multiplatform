package org.jetbrains.compose.test.tests.unit

import org.jetbrains.compose.desktop.application.internal.resolveMacSigningIdentity
import org.jetbrains.compose.desktop.application.internal.validation.ValidatedMacOSSigningSettings
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MacSignerTest {
    @Test
    fun resolvesBareIdentityToDeveloperIdApplicationCertificate() {
        val resolved = resolveMacSigningIdentity(settings(identity = "Andy Himberger")) { candidate ->
            when (candidate) {
                "Developer ID Application: Andy Himberger" ->
                    certificateOutput("Developer ID Application: Andy Himberger (GK8V53S8Z3)")

                else -> ""
            }
        }

        assertEquals("Developer ID Application: Andy Himberger (GK8V53S8Z3)", resolved.fullIdentity)
        assertTrue(resolved.isJPackageCompatible)
        assertEquals(
            listOf("Developer ID Installer: Andy Himberger (GK8V53S8Z3)"),
            resolved.installerSigningIdentityCandidates
        )
    }

    @Test
    fun resolvesBareIdentityToMacDevelopmentCertificate() {
        val resolved = resolveMacSigningIdentity(settings(identity = "Andy Himberger")) { candidate ->
            when (candidate) {
                "Mac Development: Andy Himberger" ->
                    certificateOutput("Mac Development: Andy Himberger (GK8V53S8Z3)")

                else -> ""
            }
        }

        assertEquals("Mac Development: Andy Himberger (GK8V53S8Z3)", resolved.fullIdentity)
        assertFalse(resolved.isJPackageCompatible)
        assertTrue(resolved.installerSigningIdentityCandidates.isEmpty())
    }

    @Test
    fun keepsExplicitAppleDevelopmentIdentity() {
        val resolved = resolveMacSigningIdentity(
            settings(identity = "Apple Development: Andy Himberger (GK8V53S8Z3)")
        ) { candidate ->
            if (candidate == "Apple Development: Andy Himberger (GK8V53S8Z3)") {
                certificateOutput(candidate)
            } else {
                ""
            }
        }

        assertEquals("Apple Development: Andy Himberger (GK8V53S8Z3)", resolved.fullIdentity)
        assertFalse(resolved.isJPackageCompatible)
        assertTrue(resolved.installerSigningIdentityCandidates.isEmpty())
    }

    @Test
    fun keepsExplicitAppleDistributionIdentity() {
        val resolved = resolveMacSigningIdentity(
            settings(identity = "Apple Distribution: Andy Himberger (GK8V53S8Z3)")
        ) { candidate ->
            if (candidate == "Apple Distribution: Andy Himberger (GK8V53S8Z3)") {
                certificateOutput(candidate)
            } else {
                ""
            }
        }

        assertEquals("Apple Distribution: Andy Himberger (GK8V53S8Z3)", resolved.fullIdentity)
        assertFalse(resolved.isJPackageCompatible)
        assertEquals(
            listOf(
                "3rd Party Mac Developer Installer: Andy Himberger (GK8V53S8Z3)",
                "Mac Installer Distribution: Andy Himberger (GK8V53S8Z3)"
            ),
            resolved.installerSigningIdentityCandidates
        )
    }

    @Test
    fun resolvesPkgInstallerCandidatesForDistributionCertificates() {
        val resolved = resolveMacSigningIdentity(
            settings(identity = "Mac App Distribution: Andy Himberger (GK8V53S8Z3)")
        ) { candidate ->
            if (candidate == "Mac App Distribution: Andy Himberger (GK8V53S8Z3)") {
                certificateOutput(candidate)
            } else {
                ""
            }
        }

        assertEquals(
            listOf(
                "3rd Party Mac Developer Installer: Andy Himberger (GK8V53S8Z3)",
                "Mac Installer Distribution: Andy Himberger (GK8V53S8Z3)"
            ),
            resolved.installerSigningIdentityCandidates
        )
    }

    @Test
    fun failsWhenBareIdentityMatchesMultipleCertificates() {
        val error = assertFailsWith<IllegalStateException> {
            resolveMacSigningIdentity(settings(identity = "Andy Himberger")) { candidate ->
                when (candidate) {
                    "Apple Development: Andy Himberger" ->
                        certificateOutput("Apple Development: Andy Himberger (GK8V53S8Z3)")

                    "Mac Development: Andy Himberger" ->
                        certificateOutput("Mac Development: Andy Himberger (GK8V53S8Z3)")

                    else -> ""
                }
            }
        }

        assertContains(error.message.orEmpty(), "Multiple matching certificates are found")
        assertContains(error.message.orEmpty(), "Specify the full certificate identity")
    }

    @Test
    fun failsWhenIdentityCannotBeResolved() {
        val error = assertFailsWith<IllegalStateException> {
            resolveMacSigningIdentity(settings(identity = "Andy Himberger")) { "" }
        }

        assertContains(error.message.orEmpty(), "Could not find a matching app signing certificate")
        assertContains(error.message.orEmpty(), "Developer ID Application: Andy Himberger")
        assertContains(error.message.orEmpty(), "Mac Development: Andy Himberger")
    }

    @Test
    fun ignoresSubstringMatchesThatDoNotMatchCandidateIdentity() {
        val error = assertFailsWith<IllegalStateException> {
            resolveMacSigningIdentity(settings(identity = "Andy Himberger")) { candidate ->
                when (candidate) {
                    "Apple Development: Andy Himberger" ->
                        certificateOutput("Apple Development: Andy Himberger II (GK8V53S8Z3)")

                    else -> ""
                }
            }
        }

        assertContains(error.message.orEmpty(), "Could not find a matching app signing certificate")
    }

    @Test
    fun rejectsInstallerCertificatesAsAppSigningIdentities() {
        val error = assertFailsWith<IllegalStateException> {
            resolveMacSigningIdentity(
                settings(identity = "Developer ID Installer: Andy Himberger (GK8V53S8Z3)")
            ) { "" }
        }

        assertContains(error.message.orEmpty(), "is not an app signing certificate")
    }

    private fun settings(identity: String) = ValidatedMacOSSigningSettings(
        bundleID = "com.example.app",
        identity = identity,
        keychain = null,
        prefix = "com.example."
    )

    private fun certificateOutput(alias: String): String = """
        keychain: "/Users/test/Library/Keychains/login.keychain-db"
        version: 512
        class: 0x80001000
        attributes:
            "alis"<blob>="$alias"
    """.trimIndent()
}
