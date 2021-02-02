package org.jetbrains.compose.desktop.application.internal.validation

import org.gradle.api.provider.Provider
import org.jetbrains.compose.desktop.application.dsl.MacOSSigningSettings
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.desktop.application.internal.OS
import org.jetbrains.compose.desktop.application.internal.currentOS
import java.io.File

internal data class ValidatedMacOSSigningSettings(
    val bundleID: String,
    val identity: String,
    val keychain: File?,
    val prefix: String,
)

internal fun MacOSSigningSettings.validate(
    bundleIDProvider: Provider<String?>
): ValidatedMacOSSigningSettings {
    check(currentOS == OS.MacOS) { ERR_WRONG_OS }

    val bundleID = validateBundleID(bundleIDProvider)
    val signPrefix = this.prefix.orNull
        ?: (bundleID.substringBeforeLast(".") + ".").takeIf { bundleID.contains('.') }
        ?: error(ERR_UNKNOWN_PREFIX)
    val signIdentity = this.identity.orNull
        ?: error(ERR_UNKNOWN_SIGN_ID)
    val keychainFile = this.keychain.orNull?.let { File(it) }
    if (keychainFile != null) {
        check(keychainFile.exists()) {
            "$ERR_PREFIX keychain is not an existing file: ${keychainFile.absolutePath}"
        }
    }

    return ValidatedMacOSSigningSettings(
        bundleID = bundleID,
        identity = fullDeveloperID(signIdentity),
        keychain = keychainFile,
        prefix = signPrefix
    )
}

private fun fullDeveloperID(identity: String): String {
    val developerIdPrefix = "Developer ID Application: "
    val thirdPartyMacDeveloperPrefix = "3rd Party Mac Developer Application: "
    return when {
        identity.startsWith(developerIdPrefix) -> identity
        identity.startsWith(thirdPartyMacDeveloperPrefix) -> identity
        else -> developerIdPrefix + identity
    }
}

private const val ERR_PREFIX = "Signing settings error:"
private val ERR_WRONG_OS =
    "$ERR_PREFIX macOS was expected, actual OS is $currentOS"
private val ERR_UNKNOWN_PREFIX =
    """|$ERR_PREFIX Could not infer signing prefix. To specify:
       |  * Set bundleID to reverse DNS notation (e.g. "com.mycompany.myapp");
       |  * Use '${ComposeProperties.MAC_SIGN_PREFIX}' Gradle property;
       |  * Use 'nativeExecutables.macOS.signing.prefix' DSL property;
    """.trimMargin()
private val ERR_UNKNOWN_SIGN_ID =
    """|$ERR_PREFIX signing identity is null or empty. To specify:
       |  * Use '${ComposeProperties.MAC_SIGN_ID}' Gradle property;
       |  * Use 'nativeExecutables.macOS.signing.identity' DSL property;
    """.trimMargin()
