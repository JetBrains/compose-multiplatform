package org.jetbrains.compose.desktop.application.internal.validation

import org.gradle.api.provider.Provider
import org.jetbrains.compose.desktop.application.dsl.MacOSNotarizationSettings
import org.jetbrains.compose.desktop.application.internal.ComposeProperties

internal data class ValidatedMacOSNotarizationSettings(
    val bundleID: String,
    val appleID: String,
    val password: String
)

internal fun MacOSNotarizationSettings.validate(
    bundleIDProvider: Provider<String?>
): ValidatedMacOSNotarizationSettings {
    val bundleID = validateBundleID(bundleIDProvider)
    check(!appleID.orNull.isNullOrEmpty()) {
        ERR_APPLE_ID_IS_EMPTY
    }
    check(!password.orNull.isNullOrEmpty()) {
        ERR_PASSWORD_IS_EMPTY
    }
    return ValidatedMacOSNotarizationSettings(
        bundleID = bundleID,
        appleID = appleID.orNull!!,
        password = password.orNull!!
    )
}

private const val ERR_PREFIX = "Notarization settings error:"
private val ERR_APPLE_ID_IS_EMPTY =
    """|$ERR_PREFIX appleID is null or empty. To specify:
               |  * Use '${ComposeProperties.MAC_NOTARIZATION_APPLE_ID}' Gradle property;
               |  * Or use 'nativeDistributions.macOS.notarization.appleID' DSL property;
            """.trimMargin()
private val ERR_PASSWORD_IS_EMPTY =
    """|$ERR_PREFIX password is null or empty. To specify:
               |  * Use '${ComposeProperties.MAC_NOTARIZATION_PASSWORD}' Gradle property;
               |  * Or use 'nativeDistributions.macOS.notarization.password' DSL property;
            """.trimMargin()