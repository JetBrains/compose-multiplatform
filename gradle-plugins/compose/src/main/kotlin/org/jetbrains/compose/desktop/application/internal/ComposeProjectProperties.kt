/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.compose.internal.utils.findProperty
import org.jetbrains.compose.internal.utils.toBooleanProvider

internal object ComposeProperties {
    internal const val VERBOSE = "compose.desktop.verbose"
    internal const val PRESERVE_WD = "compose.preserve.working.dir"
    internal const val MAC_SIGN = "compose.desktop.mac.sign"
    internal const val MAC_SIGN_ID = "compose.desktop.mac.signing.identity"
    internal const val MAC_SIGN_KEYCHAIN = "compose.desktop.mac.signing.keychain"
    internal const val MAC_SIGN_PREFIX = "compose.desktop.mac.signing.prefix"
    internal const val MAC_NOTARIZATION_APPLE_ID = "compose.desktop.mac.notarization.appleID"
    internal const val MAC_NOTARIZATION_PASSWORD = "compose.desktop.mac.notarization.password"
    internal const val MAC_NOTARIZATION_ASC_PROVIDER = "compose.desktop.mac.notarization.ascProvider"

    fun isVerbose(providers: ProviderFactory): Provider<Boolean> =
        providers.findProperty(VERBOSE).toBooleanProvider(false)

    fun preserveWorkingDir(providers: ProviderFactory): Provider<Boolean> =
        providers.findProperty(PRESERVE_WD).toBooleanProvider(false)

    fun macSign(providers: ProviderFactory): Provider<Boolean> =
        providers.findProperty(MAC_SIGN).toBooleanProvider(false)

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

    fun macNotarizationAscProvider(providers: ProviderFactory): Provider<String?> =
        providers.findProperty(MAC_NOTARIZATION_ASC_PROVIDER)
}