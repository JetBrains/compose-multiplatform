package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

internal object ComposeProperties {
    internal const val VERBOSE = "compose.desktop.verbose"
    internal const val PRESERVE_WD = "compose.preserve.working.dir"
    internal const val MAC_SIGN = "compose.desktop.mac.sign"
    internal const val MAC_SIGN_ID = "compose.desktop.mac.signing.identity"
    internal const val MAC_SIGN_KEYCHAIN = "compose.desktop.mac.signing.keychain"
    internal const val MAC_SIGN_PREFIX = "compose.desktop.mac.signing.prefix"
    internal const val MAC_NOTARIZATION_APPLE_ID = "compose.desktop.mac.notarization.appleID"
    internal const val MAC_NOTARIZATION_PASSWORD = "compose.desktop.mac.notarization.password"

    fun isVerbose(providers: ProviderFactory): Provider<Boolean> =
        providers.findProperty(VERBOSE).toBoolean()

    fun preserveWorkingDir(providers: ProviderFactory): Provider<Boolean> =
        providers.findProperty(PRESERVE_WD).toBoolean()

    fun macSign(providers: ProviderFactory): Provider<Boolean> =
        providers.findProperty(MAC_SIGN).toBoolean()

    fun macSignIdentity(providers: ProviderFactory): Provider<String?> =
        providers.findProperty(MAC_SIGN_ID)

    fun macSignKeychain(providers: ProviderFactory): Provider<String?> =
        providers.findProperty(MAC_SIGN_KEYCHAIN)

    fun macSignPrefix(providers: ProviderFactory): Provider<String?> =
        providers.findProperty(MAC_SIGN_PREFIX)

    fun macNotarizationAppleID(providers: ProviderFactory): Provider<String?> =
        providers.findProperty(MAC_NOTARIZATION_APPLE_ID)

    fun macNotarizationPassword(providers: ProviderFactory): Provider<String?> =
        providers.findProperty(MAC_NOTARIZATION_PASSWORD)

    private fun ProviderFactory.findProperty(prop: String): Provider<String?> =
        provider {
            gradleProperty(prop).forUseAtConfigurationTimeSafe().orNull
        }

    private fun Provider<String?>.forUseAtConfigurationTimeSafe(): Provider<String?> =
        try {
            forUseAtConfigurationTime()
        } catch (e: NoSuchMethodError) {
            // todo: remove once we drop support for Gradle 6.4
            this
        }

    private fun Provider<String?>.toBoolean(): Provider<Boolean> =
        orElse("false").map { "true" == it }
}