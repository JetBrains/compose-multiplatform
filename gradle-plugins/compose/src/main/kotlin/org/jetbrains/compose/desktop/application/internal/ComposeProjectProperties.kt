package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

internal object ComposeProperties {
    internal const val VERBOSE = "compose.desktop.verbose"
    internal const val PRESERVE_WD = "compose.desktop.verbose"
    internal const val MAC_SIGN = "compose.desktop.mac.sign"
    internal const val MAC_SIGN_ID = "compose.desktop.mac.signing.identity"
    internal const val MAC_SIGN_KEYCHAIN = "compose.desktop.mac.signing.keychain"
    internal const val MAC_SIGN_PREFIX = "compose.desktop.mac.signing.prefix"
    internal const val MAC_NOTARIZATION_APPLE_ID = "compose.desktop.mac.notarization.appleID"
    internal const val MAC_NOTARIZATION_PASSWORD = "compose.desktop.mac.notarization.password"

    fun isVerbose(providers: ProviderFactory): Provider<Boolean> =
        providers.findProperty(VERBOSE).map { "true" == it }

    fun preserveWorkingDir(providers: ProviderFactory): Provider<Boolean> =
        providers.findProperty(PRESERVE_WD).map { "true" == it }

    fun macSign(providers: ProviderFactory): Provider<Boolean> =
        providers.findProperty(MAC_SIGN).map { "true" == it }

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
            gradleProperty(prop).forUseAtConfigurationTime().orNull
        }
}